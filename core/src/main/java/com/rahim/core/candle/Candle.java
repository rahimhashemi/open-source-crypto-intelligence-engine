package com.rahim.core.candle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Candle(String market, Instant openTime, BigDecimal open, BigDecimal high,
                     BigDecimal low, BigDecimal close, BigDecimal volume) {
    public Candle {
        if (market == null || !market.matches("[A-Z0-9]{2,32}")) {
            throw new IllegalArgumentException("Invalid market");
        }
        Objects.requireNonNull(openTime, "openTime");
        validateNumber(open, "open", true);
        validateNumber(high, "high", true);
        validateNumber(low, "low", true);
        validateNumber(close, "close", true);
        validateNumber(volume, "volume", false);
        if (high.compareTo(low) < 0 || open.compareTo(low) < 0 || open.compareTo(high) > 0
                || close.compareTo(low) < 0 || close.compareTo(high) > 0) {
            throw new IllegalArgumentException("Inconsistent OHLC values");
        }
    }

    private static void validateNumber(BigDecimal value, String field, boolean positive) {
        if (value == null || (positive ? value.signum() <= 0 : value.signum() < 0)) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() > 18 || normalized.precision() - normalized.scale() > 20) {
            throw new IllegalArgumentException(field + " exceeds NUMERIC(38,18)");
        }
    }
}
