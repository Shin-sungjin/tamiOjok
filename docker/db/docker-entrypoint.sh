#!/bin/bash
set -e

PGDATA="${PGDATA:-/var/lib/pgsql/16/data}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
POSTGRES_DB="${POSTGRES_DB:-$POSTGRES_USER}"

if [ -z "$(ls -A "$PGDATA" 2>/dev/null)" ]; then
  PWFILE="$(mktemp)"
  echo "$POSTGRES_PASSWORD" > "$PWFILE"
  initdb -D "$PGDATA" --username="$POSTGRES_USER" --pwfile="$PWFILE" --auth=scram-sha-256 --auth-host=scram-sha-256
  rm -f "$PWFILE"

  echo "listen_addresses = '*'" >> "$PGDATA/postgresql.conf"
  echo "host all all 0.0.0.0/0 scram-sha-256" >> "$PGDATA/pg_hba.conf"

  pg_ctl -D "$PGDATA" -o "-c listen_addresses=''" -w start

  if [ "$POSTGRES_DB" != "$POSTGRES_USER" ]; then
    PGPASSWORD="$POSTGRES_PASSWORD" psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -c "CREATE DATABASE \"$POSTGRES_DB\";"
  fi

  pg_ctl -D "$PGDATA" -m fast -w stop
fi

exec postgres -D "$PGDATA" -c listen_addresses='*'
