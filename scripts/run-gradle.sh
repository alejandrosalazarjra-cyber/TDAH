#!/usr/bin/env bash
set -euo pipefail
# Ejecuta Gradle >=8.11.1, requerido por el plugin Android 8.10.x.
if command -v gradle >/dev/null 2>&1; then
  installed=$(gradle --version | sed -n 's/^Gradle //p' | head -1)
  if python3 - "$installed" <<'PY'
import sys
try:
 version=tuple(int(v) for v in sys.argv[1].split(".")[:3])
 raise SystemExit(0 if version >= (8,11,1) else 1)
except Exception: raise SystemExit(1)
PY
  then exec gradle "$@"; fi
fi
version=8.14.3
dir="${HOME}/.cache/neurosync/gradle-${version}"
if [[ ! -x "$dir/bin/gradle" ]]; then
  mkdir -p "$(dirname "$dir")"
  temp="$(mktemp -d)"
  trap 'rm -rf "$temp"' EXIT
  curl --fail --location --retry 2 --connect-timeout 10 \
    "https://services.gradle.org/distributions/gradle-${version}-bin.zip" \
    -o "$temp/gradle.zip"
  unzip -q -t "$temp/gradle.zip" >/dev/null
  unzip -q "$temp/gradle.zip" -d "$(dirname "$dir")"
fi
exec "$dir/bin/gradle" "$@"
