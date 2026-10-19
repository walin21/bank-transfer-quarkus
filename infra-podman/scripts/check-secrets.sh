#!/usr/bin/env bash
set -Eeuo pipefail

# Resolve the project path so checks work from any current directory.
PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
# Reject sensitive files that may have been committed accidentally.
tracked_sensitive="$(git -C "${PROJECT_DIR}" ls-files | rg '(^|/)(\.env|.*\.(jwk|pem|key|p12|jks|crt|der))$' || true)"

if [[ -n "${tracked_sensitive}" ]]; then
  echo "Error: archivos sensibles versionados:" >&2
  echo "${tracked_sensitive}" >&2
  exit 1
fi

if [[ ! -f "${PROJECT_DIR}/infra-podman/.env.example" ]]; then
  echo "Error: falta infra-podman/.env.example." >&2
  exit 1
fi

if [[ -f "${PROJECT_DIR}/infra-podman/.env" ]]; then
  # shellcheck disable=SC1091
  source "${PROJECT_DIR}/infra-podman/.env"
  for required_secret in POSTGRES_PASSWORD DATABASE_PASSWORD JWT_SECRET DEMO_PASSWORD; do
    if [[ -z "${!required_secret:-}" || "${!required_secret}" == "replace-with-"* ]]; then
      echo "Error: ${required_secret} debe definirse con un valor local antes de iniciar." >&2
      exit 1
    fi
  done
else
  echo "Error: .env aún no existe; créalo a partir de .env.example antes de iniciar." >&2
  exit 1
fi

echo "OK: no hay .env ni material criptográfico versionado."
echo "Los valores de .env.example son plantillas; define secretos locales en infra-podman/.env."
