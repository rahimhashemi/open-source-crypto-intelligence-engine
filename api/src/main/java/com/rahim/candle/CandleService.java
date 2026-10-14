package com.rahim.candle;

import com.rahim.core.candle.Candle;
import com.rahim.core.candle.CandleSource;
import com.rahim.ingest.coinex.CoinExException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

@Service
public class CandleService {
    private final CandleSource source;
    private final CandleStore store;
    private final Clock clock;

    public CandleService(CandleSource source, CandleStore store, Clock clock) {
        this.source = source;
        this.store = store;
        this.clock = clock;
    }

    public SyncResult sync(String inputMarket, String period, int limit) {
        String market = validateRequest(inputMarket, period, limit);
        // Cutoff is captured before the network request; no DB transaction spans the request.
        Instant cutoff = clock.instant();
        List<Candle> fetched = source.fetch(market, limit);
        TreeMap<Instant, Candle> closed = new TreeMap<>();
        int skippedOpen = 0;
        for (Candle candle : fetched) {
            long seconds = candle.openTime().getEpochSecond();
            if (candle.openTime().getNano() != 0 || seconds % 3600 != 0
                    || candle.openTime().isAfter(cutoff)) {
                throw new CoinExException("CoinEx returned an invalid hourly candle timestamp", false, null);
            }
            if (candle.openTime().plusSeconds(3600).isAfter(cutoff)) {
                skippedOpen++;
                continue;
            }
            Candle previous = closed.putIfAbsent(candle.openTime(), candle);
            if (previous != null && !sameValues(previous, candle)) {
                throw new CoinExException("CoinEx returned conflicting duplicate candles", false, null);
            }
        }
        int inserted = store.insertClosed(List.copyOf(closed.values()));
        return new SyncResult("coinex", market, period, fetched.size(), closed.size(), inserted,
                closed.size() - inserted, skippedOpen);
    }

    public List<Candle> read(String inputMarket, String period, int limit) {
        return store.latest(validateRequest(inputMarket, period, limit), limit);
    }

    private String validateRequest(String market, String period, int limit) {
        if (market == null || !market.matches("[A-Za-z0-9]{2,32}")) {
            throw new IllegalArgumentException("market must contain 2-32 letters or digits");
        }
        if (!"1hour".equals(period)) {
            throw new IllegalArgumentException("Only period=1hour is supported");
        }
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("limit must be between 1 and 1000");
        }
        return market.toUpperCase(Locale.ROOT);
    }

    private static boolean sameValues(Candle a, Candle b) {
        return a.open().compareTo(b.open()) == 0 && a.high().compareTo(b.high()) == 0
                && a.low().compareTo(b.low()) == 0 && a.close().compareTo(b.close()) == 0
                && a.volume().compareTo(b.volume()) == 0;
    }

    public record SyncResult(String exchange, String market, String period, int fetched,
                             int closed, int inserted, int alreadyStored, int skippedOpen) {}
}
