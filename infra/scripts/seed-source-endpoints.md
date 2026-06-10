# Source endpoint seed script

`seed-source-endpoints.sh` seeds the thesis database from `source-endpoints.json`.

## Input

The JSON file must contain an array of entries with this shape:

```json
{
  "name": "Chair of Example",
  "websiteUrl": "https://example.com/theses/"
}
```

By default, the script reads `source-endpoints.json` from the same directory as the script.

## Database connection defaults

The script uses the same defaults as the thesis database in `docker-compose.yml`:

| Variable | Default |
| --- | --- |
| `THESIS_DB_HOST` | `thesis-db` |
| `THESIS_DB_PORT` | `5432` |
| `THESIS_DB_NAME` | `thesis_db` |
| `THESIS_DB_USER` | `thesis_user` |
| `THESIS_DB_PASSWORD` | `thesis_password` |
| `SOURCE_ENDPOINTS_JSON` | `<script-directory>/source-endpoints.json` |

All variables can be overridden when running the script.

Example for local Docker port forwarding:

```sh
THESIS_DB_HOST=localhost ./infra/scripts/seed-source-endpoints.sh
```

## What the script does

1. Validates that the JSON file exists.
2. Reads the JSON file into a SQL variable.
3. Parses the entries with PostgreSQL `jsonb_to_recordset`.
4. Ignores entries with blank `name` or `websiteUrl`.
5. Deduplicates entries by `websiteUrl` within the JSON input.
6. Inserts missing rows into `chairs` using `name` and `websiteUrl`.
7. Finds both newly inserted and already existing matching chairs.
8. Inserts missing `source_endpoints` with:
   - `url = websiteUrl`
   - `status = ACTIVE`
   - `chair_id = matching chair id`
9. Skips source endpoints whose URL already exists.
10. Prints a summary with the number of input endpoints, inserted chairs, and inserted source endpoints.

The script is idempotent: running it multiple times should not create duplicate source endpoint rows.
