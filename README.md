# open-source-crypto-intelligence-engine

Open-source crypto market intelligence engine built with Java 21, Spring Boot, and PostgreSQL.

Provides:
- Market data ingestion (CoinEx)
- Historical candle storage
- Modular scoring engine
- Rotation & momentum ranking
- REST API
- Dockerized local deployment

---

## 🚀 Why This Project Exists

Most crypto tools are:
- Chart-focused
- Indicator-heavy
- Not extensible
- Not backend-oriented

Crypto Intel Engine is different.

It is a **modular market intelligence backend framework** designed for:
- Traders
- Researchers
- SaaS builders
- Quant developers
- Fintech startups

This is infrastructure — not a signal bot.

---

## 🧠 Architecture Overview

```

API (REST + DB + Scheduler)
│
CORE (Scoring & Domain Logic)
│
INGEST (CoinEx Adapter)

````

### Modules

| Module | Responsibility |
|--------|---------------|
| core | Domain models + scoring strategies |
| ingest | External data acquisition (CoinEx API) |
| api | REST endpoints + persistence + jobs |

---

## ⚙️ Tech Stack

- Java 21
- Spring Boot 3
- Maven
- PostgreSQL
- Flyway
- Docker
- OpenAPI (Swagger)

---

## 🐳 Quick Start (10 Minutes)

### 1. Clone
```bash
git clone https://github.com/
cd crypto-intel
````

### 2. Run with Docker

```bash
docker compose up --build
```

### 3. Open API Docs

```
http://localhost:8080/swagger-ui.html
```

### 4. Health Check

```
GET http://localhost:8080/health
```

---

## 📊 Available Endpoints

* `GET /health`
* `GET /scores/top`
* `GET /coins/{symbol}`
* `GET /coins/{symbol}/candles`

---

## 🔍 Scoring System

The engine uses a pluggable scoring system:

```java
interface ScoreStrategy {
    double score(MarketSeries series);
    double weight();
}
```

Current strategies:

* Relative Strength
* Volume Acceleration
* Volatility Regime

Composite score = weighted sum (0–100).

---

## 🔌 Extending the Engine

You can:

* Add new exchanges (implement adapter)
* Add new indicators
* Add new scoring strategies
* Build a frontend dashboard
* Integrate into a trading system
* Use it as a SaaS backend

---

## 📈 Roadmap

* [ ] Sector rotation engine
* [ ] Watchlist
* [ ] Alert rules
* [ ] Multi-exchange support
* [ ] Backtesting module
* [ ] WebSocket ingestion
* [ ] SaaS version

---

## 🛡 License

MIT License

---

## ⚠ Disclaimer

This project is for research and educational purposes.
It does not provide financial advice.

```