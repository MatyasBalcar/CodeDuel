#!/bin/sh
set -eu
CODEDUEL_MIGRATOR_PASSWORD=$(cat /run/secrets/migrator_password)
CODEDUEL_API_PASSWORD=$(cat /run/secrets/api_password)
CODEDUEL_WORKER_PASSWORD=$(cat /run/secrets/worker_password)
export CODEDUEL_MIGRATOR_PASSWORD CODEDUEL_API_PASSWORD CODEDUEL_WORKER_PASSWORD
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -v ON_ERROR_STOP=1 <<'SQL'
\getenv migrator_pw CODEDUEL_MIGRATOR_PASSWORD
\getenv api_pw CODEDUEL_API_PASSWORD
\getenv worker_pw CODEDUEL_WORKER_PASSWORD
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE codeduel FROM PUBLIC;
CREATE ROLE codeduel_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'migrator_pw';
CREATE ROLE codeduel_api LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'api_pw';
CREATE ROLE codeduel_worker LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'worker_pw';
GRANT CONNECT ON DATABASE codeduel TO codeduel_migrator, codeduel_api, codeduel_worker;
GRANT USAGE ON SCHEMA public TO codeduel_migrator, codeduel_api, codeduel_worker;
GRANT CREATE ON SCHEMA public TO codeduel_migrator;
SQL
