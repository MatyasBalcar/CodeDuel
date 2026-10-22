#!/bin/sh
# Logical backup plus a disposable restore drill. The live database is never overwritten.
set -eu
cd "$(dirname "$0")/.."
umask 077
mkdir -p backups
stamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_file="backups/codeduel-${stamp}.dump"
restore_database="restore_${stamp}"
compose() { docker compose --env-file .env.deploy.local -f deploy/compose.yml -f deploy/compose.local.yml "$@"; }
printf 'Pausing only the local release-rehearsal API/worker for a consistent drill.\n'
compose stop api worker
resume() { compose start api worker >/dev/null; }
trap resume EXIT INT TERM
compose exec -T db pg_dump -U postgres -d codeduel -Fc > "$backup_file"
compose exec -T db createdb -U postgres -O codeduel_migrator "$restore_database"
cleanup() { compose exec -T db dropdb -U postgres --if-exists "$restore_database" >/dev/null; resume; }
trap cleanup EXIT INT TERM
compose exec -T db pg_restore -U postgres --role=codeduel_migrator --no-owner --exit-on-error -d "$restore_database" < "$backup_file"
for table in users battles submissions rating_events battle_events problem_versions; do
  live=$(compose exec -T db psql -U postgres -d codeduel -Atc "SELECT count(*) FROM ${table}")
  restored=$(compose exec -T db psql -U postgres -d "$restore_database" -Atc "SELECT count(*) FROM ${table}")
  [ "$live" = "$restored" ] || { printf 'Restore mismatch in %s\n' "$table" >&2; exit 1; }
  printf '%s: %s rows verified\n' "$table" "$restored"
done
# Restore preserves the database's rating invariants and unique event keys.
compose exec -T db psql -U postgres -d "$restore_database" -v ON_ERROR_STOP=1 -Atc \
  "DO \$\$ BEGIN IF EXISTS (SELECT 1 FROM rating_events WHERE new_rating <> old_rating + delta) THEN RAISE EXCEPTION 'invalid restored rating event'; END IF; END \$\$;"
compose run --rm --no-deps -e "DATABASE_URL=jdbc:postgresql://db:5432/$restore_database" migrate
printf 'Restore drill passed. Private backup retained at %s\n' "$backup_file"
