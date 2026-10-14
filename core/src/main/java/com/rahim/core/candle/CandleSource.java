package com.rahim.core.candle;

import java.util.List;

public interface CandleSource {
    List<Candle> fetch(String market, int limit);
}
