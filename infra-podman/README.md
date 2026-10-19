# Infraestructura local

Esta carpeta levanta las dependencias que necesitan `transfer-api` y `transfer-consumer`: PostgreSQL, Kafka y Schema Registry. AKHQ es opcional y permite observar Kafka desde el navegador.

## Orden de dependencias

La infraestructura se organiza de menor a mayor nivel de dependencia:

1. Red compartida `bank-transfer-net`.
2. PostgreSQL, inicializado con [`scripts/01-schema.sql`](scripts/01-schema.sql) y [`scripts/02-seed.sql`](scripts/02-seed.sql).
3. Zookeeper y Kafka.
4. Schema Registry, que usa Kafka para almacenar metadatos de esquemas.
5. Tópico `bank.transfer.created` y contrato Avro [`schema-registry/TransferCreated.avsc`](schema-registry/TransferCreated.avsc).
6. `transfer-api` y `transfer-consumer`, que consumen PostgreSQL, Kafka y Schema Registry.
7. AKHQ, como herramienta opcional de observabilidad.

Los archivos Compose siguen este orden lógico: `01-kafka.yml`, `02-postgres.yml`, `03-akhq.yml` y `04-apps.yml`. Los scripts de inicio esperan la disponibilidad de cada dependencia antes de crear el tópico, registrar el esquema o arrancar las aplicaciones.

## Servicios y puertos

| Servicio | Desde el equipo | Desde la red Podman |
|---|---|---|
| PostgreSQL | `localhost:15432` | `postgres:5432` |
| Kafka | `localhost:19092` | `kafka:9092` |
| Schema Registry | `http://localhost:8083` | `http://schema-registry:8081` |
| AKHQ opcional | `http://localhost:9021` | `http://akhq:8080` |

Todos los contenedores usan la red `bank-transfer-net`.

## Configurar e iniciar

Desde `bank-transfer-quarkus/infra-podman`:

```bash
cp .env.example .env
```

Edita `.env` y define al menos:

```dotenv
POSTGRES_PASSWORD=una-clave-local-segura
DATABASE_PASSWORD=una-clave-local-segura
JWT_SECRET=un-secreto-jwt-local-largo
DEMO_PASSWORD=secret
```

`DATABASE_PASSWORD` debe coincidir con `POSTGRES_PASSWORD`. No compartas `.env`.

Comprueba que Podman esté disponible y valida los secretos:

```bash
podman info
podman compose version
./scripts/check-secrets.sh
```

Inicia solo la infraestructura:

```bash
./scripts/start-infrastructure.sh
```

Este comando reutiliza los contenedores, imágenes y volúmenes existentes. No
ejecuta una reconstrucción de las aplicaciones.

El script crea la red, inicia PostgreSQL y Kafka, espera a que Kafka y Schema Registry estén disponibles, crea el tópico configurado y registra el esquema Avro.

Para iniciar también las dos aplicaciones, desde esta misma carpeta ejecuta:

```bash
./scripts/start-all.sh
```

El arranque normal también reutiliza las imágenes de `transfer-api` y
`transfer-consumer`. Si modificaste su código o Dockerfile y necesitas crear
una nueva imagen, solicita explícitamente la reconstrucción:

```bash
REBUILD_IMAGES=true ./scripts/start-all.sh
```

Los volúmenes tienen nombres explícitos y estables (`bank-transfer-infrastructure_*`),
por lo que no dependen del directorio desde el que se invoque Compose. Los
scripts no ejecutan `down -v`; evita esa opción si necesitas conservar los
datos de PostgreSQL, Kafka o Zookeeper.

Consulta cómo funcionan las aplicaciones en [`../transfer-api/README.md`](../transfer-api/README.md) y [`../transfer-consumer/README.md`](../transfer-consumer/README.md).

## Datos iniciales

En el primer arranque de un volumen nuevo, PostgreSQL ejecuta:

- [`scripts/01-schema.sql`](scripts/01-schema.sql): tablas, restricciones e índices.
- [`scripts/02-seed.sql`](scripts/02-seed.sql): cliente, administrador, cuentas y beneficiario demo.

Los datos se conservan en el volumen `bank-transfer-infrastructure_postgres-data`. Por eso los scripts SQL no se vuelven a ejecutar al reiniciar un volumen existente.

## Esquema y tópico

El tópico por defecto es `bank.transfer.created`. El contrato Avro está en [`../contracts/avro/TransferCreated.avsc`](../contracts/avro/TransferCreated.avsc) y se registra con el subject `bank.transfer.created-value`.

Si se modifica el contrato, registra la versión disponible con:

```bash
./scripts/register-schema.sh
```

## Detener

Para detener solo la infraestructura:

```bash
./scripts/stop-infrastructure.sh
```

Para detener infraestructura y aplicaciones:

```bash
./scripts/stop-all.sh
```

Los scripts conservan los volúmenes. No elimines el volumen de PostgreSQL si necesitas mantener los datos.

## Variables configurables

Todos los valores disponibles están documentados en [`.env.example`](.env.example), incluyendo puertos, tópico Kafka, grupo del consumer, URL del servicio de tipo de cambio, issuer y expiración del JWT.
