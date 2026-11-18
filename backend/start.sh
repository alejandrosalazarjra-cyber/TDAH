#!/bin/sh
set -eu
# Solo se usa con la base de datos de pruebas gestionada por este Blueprint.
NEUROSYNC_PUBLIC_DB_CONFIRM=yes node migrate.js
exec node server.js
