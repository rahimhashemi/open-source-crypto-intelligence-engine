package com.rahim.core.candle;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class CandleTest {
    private Candle candle(String open, String high, String low, String close, String volume) {
        return new Candle("BTCUSDT", Instant.parse("2026-10-14T10:00:00Z"),
                new BigDecimal(open), new BigDecimal(high), new BigDecimal(low),
                new BigDecimal(close), new BigDecimal(volume));
    }

    @Test void acceptsExactDecimalsAndZeroVolume() {
        assertEquals(new BigDecimal("100.123456789012345678"),
                candle("100.123456789012345678", "101", "99", "100", "0").open());
    }

    @Test void rejectsInvalidOhlcvAndStorageOverflow() {
        assertThrows(IllegalArgumentException.class, () -> candle("100", "99", "90", "95", "1"));
        assertThrows(IllegalArgumentException.class, () -> candle("100", "110", "90", "100", "-1"));
        assertThrows(IllegalArgumentException.class, () -> candle("0", "110", "0", "100", "1"));
        assertThrows(IllegalArgumentException.class, () -> candle("0.0000000000000000001", "110", "0.1", "100", "1"));
        assertThrows(IllegalArgumentException.class, () -> candle("100000000000000000000", "110", "90", "100", "1"));
    }
}
