-- Existing candles are attributed to CoinEx, the project's original exchange.
ALTER TABLE candle ADD COLUMN exchange VARCHAR(16) NOT NULL DEFAULT 'coinex';
ALTER TABLE candle DROP CONSTRAINT candle_market_period_open_time_key;
ALTER TABLE candle ADD CONSTRAINT uq_candle_exchange_market_period_time
    UNIQUE (exchange, market, period, open_time);
ALTER TABLE candle ADD CONSTRAINT ck_candle_ohlcv
    CHECK (open > 0 AND high > 0 AND low > 0 AND close > 0 AND volume >= 0
        AND high >= low AND open BETWEEN low AND high AND close BETWEEN low AND high
        AND open != 'NaN'::numeric AND high != 'NaN'::numeric AND low != 'NaN'::numeric
        AND close != 'NaN'::numeric AND volume != 'NaN'::numeric);
-- The unique index supports the same prefix and backward time scans.
DROP INDEX idx_candle_market_period_time;
