#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
PROJECT_DIR="$(cd -- "${INFRA_DIR}/.." && pwd)"

# The infrastructure must be healthy before building and starting applications.
if [[ ! -f "${INFRA_DIR}/.env" ]]; then
  echo "Error: crea infra-podman/.env a partir de infra-podman/.env.example." >&2
  exit 1
fi

"${SCRIPT_DIR}/start-infrastructure.sh"

# Build application artifacts after their runtime dependencies are available.
echo "Empaquetando transfer-api..."
(cd "${PROJECT_DIR}/transfer-api" && ./mvnw package -DskipTests)
echo "Empaquetando transfer-consumer..."
(cd "${PROJECT_DIR}/transfer-consumer" && ./mvnw package -DskipTests)

set -a
# shellcheck disable=SC1091
source "${INFRA_DIR}/.env"
set +a

COMPOSE=(podman compose -p bank-transfer-infrastructure
  -f "${INFRA_DIR}/compose/01-kafka.yml"
  -f "${INFRA_DIR}/compose/02-postgres.yml"
  -f "${INFRA_DIR}/compose/03-akhq.yml"
  -f "${INFRA_DIR}/compose/04-apps.yml")

# `up --build` crea una nueva imagen de la aplicación en cada ejecución y
# puede dejar imágenes antiguas sin referencia. El arranque normal reutiliza
# las imágenes existentes; usa REBUILD_IMAGES=true solo cuando sea necesario
# incorporar cambios de código o del Dockerfile.
NEEDS_BUILD=false
for image in localhost/bank-transfer-api:latest localhost/bank-transfer-consumer:latest; do
  if ! podman image exists "${image}"; then
    NEEDS_BUILD=true
    break
  fi
done

if [[ "${REBUILD_IMAGES:-false}" == "true" || "${NEEDS_BUILD}" == "true" ]]; then
  echo "Reconstruyendo imágenes de las aplicaciones..."
  "${COMPOSE[@]}" build transfer-api transfer-consumer
else
  echo "Reutilizando imágenes existentes (REBUILD_IMAGES=true para reconstruir)."
fi

"${COMPOSE[@]}" up -d

echo "Servicios iniciados: API http://localhost:${TRANSFER_API_HTTP_PORT:-8081}, consumer http://localhost:${TRANSFER_CONSUMER_HTTP_PORT:-8082}, AKHQ http://localhost:${AKHQ_PORT:-9021}."
echo "Para ver logs: ${COMPOSE[*]} logs -f transfer-api transfer-consumer"
