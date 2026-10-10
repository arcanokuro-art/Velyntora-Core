#!/usr/bin/env bash
# Verifica que un cambio de reorganización no modifique herramientas aprobadas.
set -euo pipefail
base="${1:-origin/main}"
head="${2:-HEAD}"
changed="$(git diff --name-only "$base" "$head")"
if printf '%s\n' "$changed" | grep -Eq '^(tools/|app/src/main/res/drawable(-nodpi)?/pinta_)'; then
  echo "ERROR: modificación de tools/ o iconos aprobados detectada." >&2
  printf '%s\n' "$changed" | grep -E '^(tools/|app/src/main/res/drawable(-nodpi)?/pinta_)' >&2
  exit 1
fi
echo "OK: sin cambios en herramientas ni iconos aprobados."
