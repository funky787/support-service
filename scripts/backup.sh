#!/usr/bin/env bash
set -euo pipefail
mkdir -p backups
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-acl' > "backups/support-$(date +%Y%m%d-%H%M%S).sql"
echo "Backup saved in backups/"
