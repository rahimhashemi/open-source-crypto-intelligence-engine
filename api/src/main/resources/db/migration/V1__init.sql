CREATE TABLE IF NOT EXISTS candle (
    id BIGSERIAL PRIMARY KEY,
    market VARCHAR(32) NOT NULL,
    period VARCHAR(16) NOT NULL,
    open_time TIMESTAMPTZ NOT NULL,

    open NUMERIC(38, 18) NOT NULL,
    high NUMERIC(38, 18) NOT NULL,
    low  NUMERIC(38, 18) NOT NULL,
    close NUMERIC(38, 18) NOT NULL,
    volume NUMERIC(38, 18) NOT NULL,

    UNIQUE (market, period, open_time)
    );

CREATE TABLE IF NOT EXISTS score_snapshot (
    id BIGSERIAL PRIMARY KEY,
    market VARCHAR(32) NOT NULL,
    period VARCHAR(16) NOT NULL,
    ts TIMESTAMPTZ NOT NULL,
    total_score DOUBLE PRECISION NOT NULL,
    components_json TEXT NOT NULL,

    UNIQUE (market, period, ts)
    );

CREATE INDEX idx_candle_market_period_time
    ON candle (market, period, open_time DESC);

CREATE INDEX idx_score_market_period_ts
    ON score_snapshot (market, period, ts DESC);