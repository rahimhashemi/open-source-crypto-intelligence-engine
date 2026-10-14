# Open-source Crypto Intelligence Engine

Java 21 / Spring Boot / PostgreSQL starter for a crypto market intelligence backend.

## Current status

This repository currently provides:
- A Spring Boot REST application with `GET /health`.
- Swagger UI and OpenAPI documentation.
- Flyway migrations for `candle` and `score_snapshot`.
- Manual CoinEx hourly candle ingestion, validation and idempotent storage.
- Reading stored candles through the REST API.
- A Maven multi-module build and Docker Compose development stack.

Scoring, ranking, automatic ingestion and historical pagination are **not implemented yet**.

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

## Fetch and read hourly candles

Once the API is running:

```bash
curl --fail -X POST 'http://localhost:8080/coins/BTCUSDT/candles/sync?period=1hour&limit=100'
curl --fail 'http://localhost:8080/coins/BTCUSDT/candles?period=1hour&limit=100'
```

The POST calls CoinEx's public `GET /v2/spot/kline`; no API key is required.
It normalizes the market to uppercase and accepts limits 1-1000. This slice
supports only `1hour`. It validates the entire response before any write,
including market, millisecond timestamps, hourly alignment, decimal capacity
and OHLC/volume invariants. Future timestamps and conflicting duplicates fail
the whole request. Current unfinished candles are validated but skipped.

The summary contains `fetched`, `closed` (distinct closed candles), `inserted`,
`alreadyStored` and `skippedOpen`. Closed candles are immutable in this slice:
a repeat or concurrent sync uses PostgreSQL `ON CONFLICT DO NOTHING`.
Existing values are not overwritten, including historical corrections from
the provider. An empty successful response stores nothing.

The GET reads only PostgreSQL and returns the latest requested number of
stored candles in ascending time order. An empty database returns `[]`.
Times are UTC ISO-8601; prices and volume retain NUMERIC(38,18) precision.
Clients requiring exact arithmetic should parse JSON numbers as decimals.

Bad parameters return 400. CoinEx HTTP/business errors or invalid data return
502; request timeout returns 504. There are no automatic retries or scheduled
jobs. The default connection/request timeout is 5s, configurable through
`COINEX_TIMEOUT` (positive, at most 30s). `COINEX_BASE_URL` is a trusted
operator setting, not a request parameter.

This is a local development API: the manual sync endpoint has no authentication
or rate limiter. Add access control and rate limiting before exposing it publicly.
The Compose ports remain bound to localhost.

V2 attributes existing candles to CoinEx and adds exchange-aware uniqueness and
OHLC constraints without modifying V1. Migration fails if existing records
violate those invariants; inspect and repair such data before upgrading.

## Modules

| Module | Intended responsibility | Current implementation |
| --- | --- | --- |
| core | Domain models and scoring strategies | Candle domain and source contract |
| ingest | Exchange adapters and data ingestion | CoinEx HTTP adapter |
| api | REST, persistence, jobs and application wiring | REST sync/read, JDBC storage, health and migrations |

The core module has no Spring or persistence dependencies. The API is the only
deployable Spring Boot module.

## Continuous integration

Pull requests and pushes to `master` run:
1. Maven `clean verify` without skipping tests.
2. Verification that the API archive contains Spring Boot classes and dependencies.
3. A clean Docker Compose build and startup.
4. HTTP checks for health, OpenAPI and Swagger UI.
5. PostgreSQL checks for migrated tables and successful Flyway V1/V2 migrations.

Unit tests validate the candle domain and HTTP error/timeout handling. API integration
tests use Testcontainers PostgreSQL and a local fake CoinEx HTTP server, covering
sync/read, repeated and concurrent requests, open candles, invalid responses,
transaction rollback and database constraints. Maven tests require Docker. The
container image build skips tests; CI runs them before building the image. No CI
test relies on availability of the live exchange.

## Roadmap

- Historical pagination and additional candle periods.
- Market metadata REST endpoints.
- Scoring strategies, momentum ranking and score snapshots.
- Data validation and tests for exchange failures and duplicate ingestion.
- Additional exchanges, watchlists, alerts and backtesting.

## License

MIT. See [LICENSE](LICENSE).
