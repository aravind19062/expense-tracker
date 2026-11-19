#!/bin/sh
# Runs once, on first start of an empty data volume.
# Database-per-service: each service gets its own database and least-privilege login.
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  -v auth_pw="$AUTH_DB_PASSWORD" -v expense_pw="$EXPENSE_DB_PASSWORD" <<'SQL'
CREATE ROLE auth_user LOGIN PASSWORD :'auth_pw';
CREATE DATABASE auth_db OWNER auth_user;
CREATE ROLE expense_user LOGIN PASSWORD :'expense_pw';
CREATE DATABASE expense_db OWNER expense_user;
REVOKE ALL ON DATABASE auth_db FROM PUBLIC;
REVOKE ALL ON DATABASE expense_db FROM PUBLIC;
SQL
