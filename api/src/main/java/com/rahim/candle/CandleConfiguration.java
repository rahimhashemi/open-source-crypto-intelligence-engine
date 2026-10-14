package com.rahim.candle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahim.core.candle.CandleSource;
import com.rahim.ingest.coinex.CoinExCandleSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class CandleConfiguration {
    @Bean
    public Clock candleClock() {
        return Clock.systemUTC();
    }

    @Bean
    public CandleSource candleSource(ObjectMapper mapper,
                                    @Value("${coinex.base-url:https://api.coinex.com/v2}") URI baseUri,
                                    @Value("${coinex.timeout:5s}") Duration timeout) {
        if (timeout.isZero() || timeout.isNegative() || timeout.compareTo(Duration.ofSeconds(30)) > 0) {
            throw new IllegalArgumentException("coinex.timeout must be positive and at most 30s");
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return new CoinExCandleSource(client, mapper, baseUri, timeout);
    }
}
