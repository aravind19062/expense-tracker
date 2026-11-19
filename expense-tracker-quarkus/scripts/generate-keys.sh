#!/usr/bin/env sh
# Generates the RSA key pair used by auth-service to sign JWTs.
# Output goes to ./keys (git-ignored). Never commit these files.
set -eu
cd "$(dirname "$0")/.."
mkdir -p keys
chmod 700 keys
if [ -f keys/privateKey.pem ] && [ "${1:-}" != "--force" ]; then
  echo "keys/privateKey.pem already exists (use --force to replace it; this invalidates all issued tokens)."
  exit 0
fi
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/privateKey.pem 2>/dev/null
openssl pkey -in keys/privateKey.pem -pubout -out keys/publicKey.pem
# Readable by the non-root container user through the bind mount; the directory itself stays 700.
chmod 644 keys/privateKey.pem keys/publicKey.pem
echo "Generated keys/privateKey.pem (PKCS#8) and keys/publicKey.pem"
