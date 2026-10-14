# Open-source Crypto Intelligence Engine

Java 21 / Spring Boot / PostgreSQL starter for a crypto market intelligence backend.

## Current status

This repository currently provides:
- A Spring Boot REST application with `GET /health`.
- Swagger UI and OpenAPI documentation.
- Flyway migrations for `candle` and `score_snapshot`.
- A Maven multi-module build and Docker Compose development stack.

CoinEx ingestion, scoring, ranking and market endpoints are **not implemented yet**.

## Quick start with Docker

Requires Docker Engine or Docker Desktop and the Docker Compose v2 plugin.
No local Java or Maven installation is needed.

```bash
git clone https://github.com/rahimhashemi/open-source-crypto-intelligence-engine.git
cd open-source-crypto-intelligence-engine
docker compose up --build -d
docker compose logs -f api
```

The API starts after PostgreSQL passes its health check. The first build downloads
Maven dependencies and container images; allow time for startup.

- API: http://localhost:8080/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs

```bash
curl --fail http://localhost:8080/health
```

Expected response (JSON key order may differ):
```json
{"status":"ok","service":"crypto-intel"}
```

The health endpoint checks HTTP availability only; it does not check database
readiness after startup.

### Configuration

Defaults work without an environment file. To override them:
```bash
cp .env.example .env
```

Edit `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_PORT` or `API_PORT`,
then start the stack. Ports are bound to localhost. These default credentials
are for local development. PostgreSQL initialization variables apply only to
a new data volume; changing them does not update existing database credentials.

If port 5432 is occupied, set `DB_PORT=5433`. The API still connects to
`db:5432` inside the Compose network. Use the configured `API_PORT` in browser
and curl URLs.

### Stop and restart

```bash
docker compose down
docker compose up -d
```

The named database volume survives `down`. To delete **all local database data**
and initialize from scratch:
```bash
docker compose down --volumes
docker compose up --build -d
```

## Run with local Java and Maven

Requires JDK 21 and Maven 3.9.x. Run commands from the repository root:
```bash
docker compose up -d db
mvn -B -ntp clean verify
java -jar api/target/crypto-intel.jar
```

The application defaults to `localhost:5432/cryptointel` with local credentials
`cryptointel` / `cryptointel`. For custom database settings, export
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and
`SPRING_DATASOURCE_PASSWORD` before running Java. Compose reads `.env`; a locally
started JVM does not automatically read it.

Flyway applies migrations on startup. Hibernate validates the schema and does
not create it.

## Modules

| Module | Intended responsibility | Current implementation |
| --- | --- | --- |
| core | Domain models and scoring strategies | Placeholder |
| ingest | Exchange adapters and data ingestion | Placeholder |
| api | REST, persistence, jobs and application wiring | Bootstrap, health, OpenAPI and migrations |

The core and ingest modules still contain IDE-generated sample classes.
The API is the only deployable Spring Boot module.

## Continuous integration

Pull requests and pushes to `master` run:
1. Maven `clean verify` without skipping tests.
2. Verification that the API archive contains Spring Boot classes and dependencies.
3. A clean Docker Compose build and startup.
4. HTTP checks for health, OpenAPI and Swagger UI.
5. PostgreSQL checks for migrated tables and a successful Flyway V1 migration.

There are currently no unit tests. The CI smoke checks validate application
packaging and startup, not future market or scoring behavior.

## Roadmap

- CoinEx historical candle ingestion and idempotent persistence.
- Market and candle REST endpoints.
- Scoring strategies, momentum ranking and score snapshots.
- Data validation and tests for exchange failures and duplicate ingestion.
- Additional exchanges, watchlists, alerts and backtesting.

## License

MIT. See [LICENSE](LICENSE).
