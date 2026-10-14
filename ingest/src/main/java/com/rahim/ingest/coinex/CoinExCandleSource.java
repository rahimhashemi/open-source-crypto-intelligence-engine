package com.rahim.ingest.coinex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahim.core.candle.Candle;
import com.rahim.core.candle.CandleSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class CoinExCandleSource implements CandleSource {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final URI baseUri;
    private final Duration timeout;

    public CoinExCandleSource(HttpClient client, ObjectMapper mapper, URI baseUri, Duration timeout) {
        this.client = client;
        this.mapper = mapper;
        this.baseUri = baseUri;
        this.timeout = timeout;
    }

    @Override
    public List<Candle> fetch(String market, int limit) {
        if (market == null || !market.matches("[A-Z0-9]{2,32}") || limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("Invalid candle request");
        }
        URI uri = URI.create(baseUri.toString().replaceAll("/+$", "")
                + "/spot/kline?market=" + market + "&period=1hour&limit=" + limit);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout)
                .header("Accept", "application/json").GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new CoinExException("CoinEx returned an unsuccessful HTTP status", false, null);
            }
            JsonNode root = mapper.readTree(response.body());
            if (root == null || !root.path("code").isIntegralNumber() || !root.path("code").canConvertToLong()
                    || root.path("code").longValue() != 0 || !root.path("data").isArray()
                    || root.path("data").size() > limit) {
                throw new CoinExException("CoinEx returned an invalid or unsuccessful response", false, null);
            }
            List<Candle> candles = new ArrayList<>();
            for (JsonNode item : root.path("data")) {
                if (!market.equals(text(item, "market"))) {
                    throw new IllegalArgumentException("Unexpected market in CoinEx response");
                }
                JsonNode timestamp = item.path("created_at");
                if (!timestamp.isIntegralNumber() || !timestamp.canConvertToLong() || timestamp.longValue() <= 0) {
                    throw new IllegalArgumentException("Invalid CoinEx timestamp");
                }
                candles.add(new Candle(market, Instant.ofEpochMilli(timestamp.longValue()),
                        number(item, "open"), number(item, "high"), number(item, "low"),
                        number(item, "close"), number(item, "volume")));
            }
            return List.copyOf(candles);
        } catch (HttpTimeoutException ex) {
            throw new CoinExException("CoinEx request timed out", true, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CoinExException("CoinEx request interrupted", false, ex);
        } catch (IOException | IllegalArgumentException ex) {
            throw new CoinExException("CoinEx response could not be read or validated", false, ex);
        }
    }

    private static String text(JsonNode item, String field) {
        JsonNode value = item.path(field);
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("Missing CoinEx field: " + field);
        }
        return value.textValue();
    }

    private static BigDecimal number(JsonNode item, String field) {
        String value = text(item, field);
        if (value.length() > 80 || !value.matches("[0-9]+(\\.[0-9]+)?")) {
            throw new IllegalArgumentException("Invalid CoinEx decimal: " + field);
        }
        return new BigDecimal(value);
    }
}
