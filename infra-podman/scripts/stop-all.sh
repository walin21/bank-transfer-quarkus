#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

# Stop the application layer and optional AKHQ before base infrastructure.
if [[ ! -f "${INFRA_DIR}/.env" ]]; then
  echo "Error: crea infra-podman/.env a partir de infra-podman/.env.example." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source "${INFRA_DIR}/.env"
set +a

podman compose -p bank-transfer-infrastructure \
  -f "${INFRA_DIR}/compose/01-kafka.yml" \
  -f "${INFRA_DIR}/compose/02-postgres.yml" \
  -f "${INFRA_DIR}/compose/03-akhq.yml" \
  -f "${INFRA_DIR}/compose/04-apps.yml" down

"${SCRIPT_DIR}/stop-infrastructure.sh"
