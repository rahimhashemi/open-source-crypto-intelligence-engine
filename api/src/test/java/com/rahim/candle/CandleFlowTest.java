package com.rahim.candle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(CandleFlowTest.FixedClock.class)
class CandleFlowTest {
    @Container
    static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16.6");
    static final AtomicReference<String> RESPONSE = new AtomicReference<>();
    static final AtomicInteger CALLS = new AtomicInteger();
    static final AtomicReference<String> QUERY = new AtomicReference<>();
    static final HttpServer EXCHANGE = startExchange();

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired CandleService service;
    @Autowired CandleStore store;

    static HttpServer startExchange() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v2/spot/kline", exchange -> {
                CALLS.incrementAndGet();
                QUERY.set(exchange.getRequestURI().getQuery());
                byte[] bytes = RESPONSE.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::getJdbcUrl);
        registry.add("spring.datasource.username", DB::getUsername);
        registry.add("spring.datasource.password", DB::getPassword);
        registry.add("coinex.base-url", () -> "http://127.0.0.1:" + EXCHANGE.getAddress().getPort() + "/v2");
    }

    @TestConfiguration
    static class FixedClock {
        @Bean @Primary Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-14T12:30:00Z"), ZoneOffset.UTC);
        }
    }

    @BeforeEach void reset() {
        jdbc.update("DELETE FROM candle");
        CALLS.set(0);
        RESPONSE.set(payload(candle("10:00", "100"), candle("09:00", "100"), candle("12:00", "100")));
    }

    @AfterAll static void stopExchange() {
        EXCHANGE.stop(0);
    }

    static String candle(String time, String open) {
        long ts = Instant.parse("2026-10-14T" + time + ":00Z").toEpochMilli();
        return """
                {"market":"BTCUSDT","created_at":%d,"open":"%s","high":"110","low":"90","close":"105","volume":"1.123456789012345678"}
                """.formatted(ts, open).trim();
    }

    static String payload(String... candles) {
        return "{\"code\":0,\"data\":[" + String.join(",", candles) + "],\"message\":\"OK\"}";
    }

    @Test void syncReadAndRepeatAreIdempotent() throws Exception {
        var first = rest.postForEntity("/coins/btcusdt/candles/sync", null, String.class);
        assertEquals(HttpStatus.OK, first.getStatusCode());
        JsonNode summary = mapper.readTree(first.getBody());
        assertEquals(2, summary.path("inserted").asInt());
        assertEquals(1, summary.path("skippedOpen").asInt());
        assertEquals("market=BTCUSDT&period=1hour&limit=100", QUERY.get());
        var second = rest.postForEntity("/coins/BTCUSDT/candles/sync", null, String.class);
        assertEquals(0, mapper.readTree(second.getBody()).path("inserted").asInt());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM candle", Integer.class));
        int calls = CALLS.get();
        JsonNode rows = mapper.copy().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(rest.getForObject("/coins/BTCUSDT/candles", String.class));
        assertEquals(2, rows.size());
        assertEquals("2026-10-14T09:00:00Z", rows.get(0).path("openTime").asText());
        assertEquals("2026-10-14T10:00:00Z", rows.get(1).path("openTime").asText());
        assertEquals(0, rows.get(0).path("volume").decimalValue().compareTo(new java.math.BigDecimal("1.123456789012345678")));
        JsonNode latest = mapper.readTree(rest.getForObject("/coins/BTCUSDT/candles?limit=1", String.class));
        assertEquals("2026-10-14T10:00:00Z", latest.get(0).path("openTime").asText());
        assertEquals(calls, CALLS.get(), "GET must not call CoinEx");
    }

    @Test void concurrentSyncStoresEachCandleOnce() throws Exception {
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Future<CandleService.SyncResult>> results = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                results.add(pool.submit(() -> service.sync("BTCUSDT", "1hour", 100)));
            }
            int inserted = 0;
            for (Future<CandleService.SyncResult> result : results) inserted += result.get().inserted();
            assertEquals(2, inserted);
            assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM candle", Integer.class));
        }
    }

    @Test void invalidSecondCandleDoesNotPersistValidFirstCandle() {
        RESPONSE.set(payload(candle("09:00", "100"), candle("10:00", "120")));
        assertEquals(HttpStatus.BAD_GATEWAY,
                rest.postForEntity("/coins/BTCUSDT/candles/sync", null, String.class).getStatusCode());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM candle", Integer.class));
    }

    @Test void rejectsConflictingDuplicatesAndFutureTimestamps() {
        RESPONSE.set(payload(candle("09:00", "100"), candle("09:00", "101")));
        assertEquals(HttpStatus.BAD_GATEWAY,
                rest.postForEntity("/coins/BTCUSDT/candles/sync", null, String.class).getStatusCode());
        RESPONSE.set(payload(candle("13:00", "100")));
        assertEquals(HttpStatus.BAD_GATEWAY,
                rest.postForEntity("/coins/BTCUSDT/candles/sync", null, String.class).getStatusCode());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM candle", Integer.class));
    }

    @Test void rejectsWrongMarketMillisecondsAndMalformedResponse() {
        for (String response : List.of(payload(candle("09:00", "100").replace("BTCUSDT", "ETHUSDT")),
                payload(candle("09:00", "100").replace("1791968400000", "123")),
                "{\"code\":3008,\"data\":[]}", "not-json", "{\"code\":0}")) {
            RESPONSE.set(response);
            assertEquals(HttpStatus.BAD_GATEWAY,
                    rest.postForEntity("/coins/BTCUSDT/candles/sync", null, String.class).getStatusCode());
        }
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM candle", Integer.class));
    }

    @Test void badQueryReturns400WithoutCallingExchange() {
        for (String query : List.of("?period=1day", "?limit=0", "?limit=1001", "?limit=abc")) {
            assertEquals(HttpStatus.BAD_REQUEST,
                    rest.postForEntity("/coins/BTCUSDT/candles/sync" + query, null, String.class).getStatusCode());
        }
        assertEquals(0, CALLS.get());
    }

    @Test void databaseRejectsInvalidOhlcAndStoreRollsBackWholeBatch() {
        var valid = new com.rahim.core.candle.Candle("BTCUSDT", Instant.parse("2026-10-14T09:00:00Z"),
                new java.math.BigDecimal("100"), new java.math.BigDecimal("110"),
                new java.math.BigDecimal("90"), new java.math.BigDecimal("105"), java.math.BigDecimal.ONE);
        jdbc.execute("ALTER TABLE candle ADD CONSTRAINT test_reject_second CHECK (open_time < '2026-10-14T10:00:00Z')");
        try {
            var second = new com.rahim.core.candle.Candle("BTCUSDT", Instant.parse("2026-10-14T10:00:00Z"),
                    valid.open(), valid.high(), valid.low(), valid.close(), valid.volume());
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> store.insertClosed(List.of(valid, second)));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM candle", Integer.class));
        } finally {
            jdbc.execute("ALTER TABLE candle DROP CONSTRAINT test_reject_second");
        }
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("""
                        INSERT INTO candle (market, period, open_time, open, high, low, close, volume)
                        VALUES ('BTCUSDT', '1hour', now(), 100, 99, 90, 100, 1)
                        """));
    }
}
