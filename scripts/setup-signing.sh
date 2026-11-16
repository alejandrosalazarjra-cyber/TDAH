#!/usr/bin/env bash
set -euo pipefail
# Run ONCE inside a trusted GitHub Codespace, not in GitHub Actions.
cd "$(dirname "$0")/.."
REPO="elgatojhon8-droid/TDAH"
if ! command -v gh >/dev/null || ! command -v keytool >/dev/null || ! command -v openssl >/dev/null; then
  echo "Se necesitan gh, keytool (Java) y openssl dentro de Codespaces." >&2; exit 1
fi
gh auth status >/dev/null || { echo "Inicia sesión en GitHub CLI (gh auth login)." >&2; exit 1; }
if gh secret list -R "$REPO" | grep -q '^NS_SIGNING_KEY_B64[[:space:]]'; then
  echo "GitHub ya tiene una firma de distribución. NO crees otra: cambiarla impediría actualizar la aplicación." >&2
  exit 1
fi
umask 077
mkdir -p signing
if [[ -e signing/neurosync-release.jks ]]; then
  echo "YA EXISTE una llave: nunca reemplaces una llave usada para actualizar APK." >&2
  exit 1
fi
PASSWORD="$(openssl rand -hex 32)"
keytool -genkeypair -v -alias neurosync -keyalg RSA -keysize 3072 \
  -validity 10000 -keystore signing/neurosync-release.jks \
  -storepass "$PASSWORD" -keypass "$PASSWORD" \
  -dname "CN=NeuroSync Familia, OU=Distribucion privada, O=NeuroSync, C=CO" -noprompt >/dev/null 2>&1
printf '%s' "$PASSWORD" > signing/keystore-password-KEEP-PRIVATE.txt
base64 -w 0 signing/neurosync-release.jks | gh secret set NS_SIGNING_KEY_B64 -R "$REPO"
printf '%s' "$PASSWORD" | gh secret set NS_SIGNING_PASSWORD -R "$REPO"
printf '%s' "neurosync" | gh secret set NS_SIGNING_ALIAS -R "$REPO"
unset PASSWORD
echo "Se configuraron los 3 secretos en GitHub."
echo "MUY IMPORTANTE: guarda una copia privada de signing/neurosync-release.jks y signing/keystore-password-KEEP-PRIVATE.txt."
echo "Sin estas dos piezas NO podrás actualizar aplicaciones firmadas con esta clave."
echo "NO compartas ni subas esos archivos al repositorio ni a chats."
keytool -list -keystore signing/neurosync-release.jks -alias neurosync -storepass "$(cat signing/keystore-password-KEEP-PRIVATE.txt)" | grep -E 'SHA256|SHA-256|Valid from' || true

echo "Creando una compilación definitiva con la firma estable..."
gh workflow run android-apk.yml -R "$REPO" --ref main
echo "Revisa GitHub Actions → Compilar APK NeuroSync → último resultado. El artefacto definitivo se llamará NeuroSync-Familia-Firmada."
