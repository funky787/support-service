#!/usr/bin/env bash
set -euo pipefail
FILE="${1:?Usage: scripts/restore-test.sh backup.sql}"
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS support_restore;" -c "CREATE DATABASE support_restore;"'
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d support_restore -v ON_ERROR_STOP=1' < "$FILE"
echo 'Restore validated in separate database support_restore'
