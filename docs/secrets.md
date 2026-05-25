# Secrets with SOPS + age

We commit encrypted env vars as `.env.enc`. Everyone decrypts it into their own local `.env`.

Commit:

- `.env.enc`
- `.sops.yaml`
- `.env.example`

Never commit:

- `.env`
- `~/.config/sops/age/keys.txt`

## Install

```bash
brew install sops age
```

## First setup by maintainer

Run from the repo root:

```bash
scripts/secrets/init-sops.sh
```

This creates an age key if needed, creates `.sops.yaml` if needed, and encrypts `.env` into `.env.enc`.

## Teammate setup

Create a key:

```bash
mkdir -p ~/.config/sops/age
age-keygen -o ~/.config/sops/age/keys.txt
```

Send the maintainer the public key:

```bash
grep "public key" ~/.config/sops/age/keys.txt
```

After you are added:

```bash
git pull
scripts/secrets/decrypt-env.sh
docker compose up --build
```

## Add a teammate

```bash
scripts/secrets/add-age-recipient.sh age1theirpublickey...
git add .sops.yaml .env.enc
git commit -m "Add SOPS recipient"
```

## Edit secrets

```bash
scripts/secrets/edit-env.sh
```

Then commit the changed `.env.enc`.
