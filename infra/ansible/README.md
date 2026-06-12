# Infrastructure

This directory contains infrastructure/deployment automation. The current Ansible setup can deploy the app to an already-created Azure VM from your local machine.

## Run Ansible locally against an existing VM

### 1. Install Ansible

On Arch Linux:

```bash
sudo pacman -S ansible
```

On Ubuntu/Debian:

```bash
sudo apt update
sudo apt install ansible -y
```

Check installation:

```bash
ansible --version
```

### 2. Create the Ansible inventory

Copy the template:

```bash
cp infra/ansible/inventory.ini.j2 infra/ansible/inventory.ini
```

Edit `infra/ansible/inventory.ini` and replace the placeholders:

```ini
[app]
<vm-public-ip> ansible_user=<vm-user> ansible_ssh_private_key_file=<path-to-private-key> ansible_python_interpreter=/usr/bin/python3 ansible_ssh_common_args='-o StrictHostKeyChecking=no'
```

Example:

```ini
[app]
158.158.17.74 ansible_user=azureuser ansible_ssh_private_key_file=test-vm_key.pem ansible_python_interpreter=/usr/bin/python3 ansible_ssh_common_args='-o StrictHostKeyChecking=no'
```

Test SSH first:

```bash
ssh -i test-vm_key.pem azureuser@158.158.17.74
```

If SSH does not work, Ansible will not work.

### 3. Load environment variables

If you have a local `.env` file with the required variables, load it into your shell:

```bash
set -a
source .env
set +a
```

At minimum, these variables are required:

```bash
export APP_DOMAIN="openthesisradar.<vm-public-ip>.nip.io"
export IMAGE_REPOSITORY="ghcr.io/aet-devops26/team-project95"
export IMAGE_TAG="latest"
```

Use a lowercase `APP_DOMAIN`. DNS names are case-insensitive, but Linux certificate paths are case-sensitive. Certbot stores Let's Encrypt files under a lowercase domain directory.

If Certbot is enabled, also set:

```bash
export CERTBOT_EMAIL="you@example.com"
export ENABLE_CERTBOT=true
```

For private GHCR images, also set:

```bash
export GHCR_USER="<github-username>"
export GHCR_TOKEN="<github-token-or-pat>"
```

The token needs permission to read private GHCR packages, usually `read:packages` and, for private repositories, `repo`.

Optional service configuration:

```bash
export THESIS_DB_PASSWORD="change-me"
export VECTOR_DB_PASSWORD="change-me"
export AZURE_OPENAI_API_KEY="change-me"
export OPENAI_API_KEY="change-me"
```

### 4. First test without Certbot

For the first deployment test, it can be easier to disable certificate generation:

```bash
export ENABLE_CERTBOT=false
ansible-playbook -i infra/ansible/inventory.ini infra/ansible/deploy.yml
```

Then check HTTP:

```text
http://openthesisradar.<vm-public-ip>.nip.io/
```

### 5. Deploy with Certbot enabled

Make sure ports `80` and `443` are open on the VM/network security group, then run:

```bash
export ENABLE_CERTBOT=true
ansible-playbook -i infra/ansible/inventory.ini infra/ansible/deploy.yml
```

This will:

1. install/configure Docker
2. create `/open-thesis-radar`
3. copy `docker-compose.prod.yml` to `/open-thesis-radar/docker-compose.yml`
4. write `/open-thesis-radar/.env`
5. log in to GHCR if credentials are provided
6. pull images
7. start containers with an HTTP-only Nginx config
8. request the initial Let's Encrypt certificate with Certbot
9. switch Nginx to HTTPS
10. install the Certbot renewal cron job

### 6. Why there are HTTP and HTTPS Nginx templates

```text
infra/ansible/roles/app/templates/nginx-http.conf.j2
infra/ansible/roles/certbot/templates/nginx-https.conf.j2
```

The `.j2` suffix means Ansible renders the file with real variable values before copying it to the VM.

There are two Nginx templates because the first certificate generation has a bootstrapping problem. On the first deploy, these certificate files do not exist yet:

```text
/etc/letsencrypt/live/<domain>/fullchain.pem
/etc/letsencrypt/live/<domain>/privkey.pem
```

If Nginx starts immediately with an HTTPS config that references those files, the frontend container can fail to start.

Deployment therefore works in two phases:

1. Start frontend Nginx with the HTTP-only config.
2. Serve ACME challenge files from `/.well-known/acme-challenge/`.
3. Run Certbot in webroot mode to create the certificate.
4. Replace the Nginx config with the HTTPS config.
5. Restart/recreate the frontend container so Nginx uses the certificate.

The Certbot container is not an Nginx server. It only writes and renews certificate files in shared Docker volumes. The frontend container remains the only public Nginx entrypoint.

### 7. Check running containers

```bash
ssh -i test-vm_key.pem azureuser@<vm-public-ip>
cd /open-thesis-radar
docker compose ps
```

Only the frontend should expose host ports:

```text
0.0.0.0:80->80/tcp
0.0.0.0:443->443/tcp
```

Backend services and databases should only show internal container ports.

### 8. Operate containers with Ansible

Restart a service:

```bash
export SERVICE=frontend
export ACTION=restart
ansible-playbook -i infra/ansible/inventory.ini infra/ansible/ops.yml
```

Show logs:

```bash
export SERVICE=frontend
export ACTION=logs
ansible-playbook -i infra/ansible/inventory.ini infra/ansible/ops.yml
```

Pull a new image:

```bash
export SERVICE=thesis-service
export ACTION=pull
ansible-playbook -i infra/ansible/inventory.ini infra/ansible/ops.yml
```

Supported services:

- `frontend`
- `thesis-service`
- `scraping-service`
- `vector-search-service`
- `genai-service`
- `thesis-db`
- `vector-db`
- `certbot`

Supported actions:

- `start`
- `stop`
- `restart`
- `pull`
- `logs`

### 9. Bring all containers down

From your machine:

```bash
ansible app \
  -i infra/ansible/inventory.ini \
  -b \
  -m ansible.builtin.shell \
  -a "cd /open-thesis-radar && docker compose down"
```

To also remove Docker volumes, including database data and certificates:

```bash
ansible app \
  -i infra/ansible/inventory.ini \
  -b \
  -m ansible.builtin.shell \
  -a "cd /open-thesis-radar && docker compose down -v"
```

Be careful: `down -v` deletes persistent database and certificate volumes.
