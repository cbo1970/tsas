#!/usr/bin/env bash
# Bootstrap script picked up by the official postgres image (TEN-58).
# Creates a separate "keycloak" database + role for the Keycloak prod
# storage backend, so it does not share the application's "tsas" DB.
#
# Runs only on a fresh data volume — postgres' /docker-entrypoint-initdb.d
# scripts execute exactly once during initdb.

set -euo pipefail

: "${KC_DB_NAME:?KC_DB_NAME must be set}"
: "${KC_DB_USERNAME:?KC_DB_USERNAME must be set}"
: "${KC_DB_PASSWORD:?KC_DB_PASSWORD must be set}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE ROLE "${KC_DB_USERNAME}" WITH LOGIN PASSWORD '${KC_DB_PASSWORD}';
    CREATE DATABASE "${KC_DB_NAME}" OWNER "${KC_DB_USERNAME}";
    GRANT ALL PRIVILEGES ON DATABASE "${KC_DB_NAME}" TO "${KC_DB_USERNAME}";
EOSQL
