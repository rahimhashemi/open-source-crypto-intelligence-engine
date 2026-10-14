package com.rahim.ingest.coinex;

public class CoinExException extends RuntimeException {
    private final boolean timeout;

    public CoinExException(String message, boolean timeout, Throwable cause) {
        super(message, cause);
        this.timeout = timeout;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
