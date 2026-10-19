#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

# Validate the HTTP client before waiting for the registry dependency.
if ! command -v curl >/dev/null 2>&1; then
  echo "Error: curl es necesario para registrar el esquema Avro." >&2
  exit 1
fi

if [[ -f "${INFRA_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${INFRA_DIR}/.env"
  set +a
fi
SCHEMA_FILE="${SCRIPT_DIR}/../../contracts/avro/TransferCreated.avsc"
REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://localhost:8083}"
SUBJECT="${KAFKA_TRANSFER_TOPIC:-bank.transfer.created}-value"

# Wait for Schema Registry before posting the Avro contract.
REGISTRY_READY=false
for _ in {1..30}; do
  if curl --fail --silent "${REGISTRY_URL}/subjects" >/dev/null; then
    REGISTRY_READY=true
    break
  fi
  sleep 2
done

if [[ "${REGISTRY_READY}" != true ]]; then
  echo "Error: Schema Registry no estuvo disponible después de 60 segundos." >&2
  exit 1
fi

# Serialize the Avro document as a JSON string for the registry API.
if command -v python3 >/dev/null 2>&1; then
  SCHEMA_JSON="$(python3 -c 'import json,sys; print(json.dumps(open(sys.argv[1], encoding="utf-8").read()))' "${SCHEMA_FILE}")"
else
  echo "Error: python3 es necesario para registrar el esquema Avro." >&2
  exit 1
fi
curl --fail --silent --show-error \
  -X POST "${REGISTRY_URL}/subjects/${SUBJECT}/versions" \
  -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --data "{\"schema\":${SCHEMA_JSON}}"
echo
echo "Esquema registrado en ${SUBJECT}."
