package com.rahim.candle;

import com.rahim.core.candle.Candle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class CandleStore {
    private final JdbcTemplate jdbc;

    public CandleStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int insertClosed(List<Candle> candles) {
        int inserted = 0;
        // Callers provide chronological order to give concurrent writers a consistent lock order.
        for (Candle candle : candles) {
            inserted += jdbc.update("""
                    INSERT INTO candle (exchange, market, period, open_time, open, high, low, close, volume)
                    VALUES ('coinex', ?, '1hour', ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (exchange, market, period, open_time) DO NOTHING
                    """, candle.market(), Timestamp.from(candle.openTime()), candle.open(), candle.high(),
                    candle.low(), candle.close(), candle.volume());
        }
        return inserted;
    }

    public List<Candle> latest(String market, int limit) {
        return jdbc.query("""
                SELECT * FROM (
                    SELECT market, open_time, open, high, low, close, volume
                    FROM candle WHERE exchange = 'coinex' AND market = ? AND period = '1hour'
                    ORDER BY open_time DESC LIMIT ?
                ) recent ORDER BY open_time ASC
                """, (rs, row) -> new Candle(rs.getString("market"), rs.getTimestamp("open_time").toInstant(),
                rs.getBigDecimal("open"), rs.getBigDecimal("high"), rs.getBigDecimal("low"),
                rs.getBigDecimal("close"), rs.getBigDecimal("volume")), market, limit);
    }
}
