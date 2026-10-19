#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

# Validate local tooling before starting any container.
if ! command -v podman >/dev/null 2>&1; then
  echo "Error: Podman no está instalado o no está en PATH." >&2
  exit 1
fi
if ! command -v curl >/dev/null 2>&1; then
  echo "Error: curl es necesario para comprobar y registrar Schema Registry." >&2
  exit 1
fi
if ! podman info >/dev/null 2>&1; then
  echo "Error: Podman está instalado, pero el motor no está disponible." >&2
  echo "Inicia el servicio/máquina de Podman y vuelve a ejecutar este script." >&2
  exit 1
fi

if [[ -f "${INFRA_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${INFRA_DIR}/.env"
  set +a
fi
KAFKA_PORT="${KAFKA_PORT:-19092}"
KAFKA_TOPIC="${KAFKA_TRANSFER_TOPIC:-bank.transfer.created}"

# Dependency order: network -> PostgreSQL/Kafka -> readiness -> topic/schema.
COMPOSE=(podman compose -p bank-transfer-infrastructure
  -f "${INFRA_DIR}/compose/01-kafka.yml"
  -f "${INFRA_DIR}/compose/02-postgres.yml")

if ! podman compose version >/dev/null 2>&1; then
  echo "Error: no se encontró el proveedor de Podman Compose." >&2
  echo "Instala podman-compose o habilita el proveedor compose de Podman." >&2
  exit 1
fi

podman network exists bank-transfer-net || podman network create bank-transfer-net
"${COMPOSE[@]}" up -d

# Kafka must be ready before creating the topic.
echo "Esperando Kafka..."
KAFKA_READY=false
for _ in {1..30}; do
  if podman exec bank-transfer-kafka kafka-topics --bootstrap-server "localhost:${KAFKA_PORT}" --list >/dev/null 2>&1; then
    KAFKA_READY=true
    break
  fi
  sleep 2
done

if [[ "${KAFKA_READY}" != true ]]; then
  echo "Error: Kafka no estuvo disponible después de 60 segundos." >&2
  exit 1
fi

# Schema Registry depends on Kafka and must be ready before schema registration.
echo "Esperando Schema Registry..."
SCHEMA_REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://localhost:${SCHEMA_REGISTRY_PORT:-8083}}"
SCHEMA_REGISTRY_READY=false
for _ in {1..30}; do
  if curl --fail --silent "${SCHEMA_REGISTRY_URL}/subjects" >/dev/null 2>&1; then
    SCHEMA_REGISTRY_READY=true
    break
  fi
  sleep 2
done

if [[ "${SCHEMA_REGISTRY_READY}" != true ]]; then
  echo "Error: Schema Registry no estuvo disponible después de 60 segundos." >&2
  exit 1
fi

podman exec bank-transfer-kafka kafka-topics \
  --bootstrap-server "localhost:${KAFKA_PORT}" \
  --if-not-exists \
  --create \
  --topic "${KAFKA_TOPIC}" \
  --partitions "${KAFKA_TRANSFER_PARTITIONS:-1}" \
  --replication-factor 1

"${SCRIPT_DIR}/register-schema.sh"

echo "Infraestructura base iniciada."
echo "PostgreSQL: localhost:${POSTGRES_PORT:-15432}"
echo "Kafka: localhost:${KAFKA_PORT:-19092}"
echo "Schema Registry: http://localhost:${SCHEMA_REGISTRY_PORT:-8083}"
echo "Tópico: ${KAFKA_TOPIC}"
