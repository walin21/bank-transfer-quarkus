#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

# Stop infrastructure services while preserving named data volumes.
if [[ -f "${INFRA_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${INFRA_DIR}/.env"
  set +a
fi

podman compose -p bank-transfer-infrastructure \
  -f "${INFRA_DIR}/compose/01-kafka.yml" \
  -f "${INFRA_DIR}/compose/02-postgres.yml" \
  -f "${INFRA_DIR}/compose/03-akhq.yml" \
  down

if podman network exists bank-transfer-net; then
  podman network rm bank-transfer-net
fi

echo "Infraestructura detenida. Los volúmenes se conservaron."
