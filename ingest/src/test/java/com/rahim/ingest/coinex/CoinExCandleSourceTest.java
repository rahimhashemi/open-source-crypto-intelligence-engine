package com.rahim.ingest.coinex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class CoinExCandleSourceTest {
    @Test void mapsHttpErrorsWithoutExposingResponseBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/spot/kline", exchange -> {
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        server.start();
        try {
            CoinExCandleSource source = source(server, Duration.ofSeconds(2));
            CoinExException ex = assertThrows(CoinExException.class, () -> source.fetch("BTCUSDT", 100));
            assertFalse(ex.isTimeout());
        } finally {
            server.stop(0);
        }
    }

    @Test void mapsTimeoutAndRejectsInvalidRequestBeforeNetwork() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        server.createContext("/spot/kline", exchange -> {
            try {
                Thread.sleep(500);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            CoinExCandleSource source = source(server, Duration.ofMillis(100));
            assertThrows(IllegalArgumentException.class, () -> source.fetch("../", 100));
            CoinExException ex = assertThrows(CoinExException.class, () -> source.fetch("BTCUSDT", 100));
            assertTrue(ex.isTimeout());
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    private CoinExCandleSource source(HttpServer server, Duration timeout) {
        return new CoinExCandleSource(HttpClient.newHttpClient(), new ObjectMapper(),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()), timeout);
    }
}
