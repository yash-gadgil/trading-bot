package io.github.yash_gadgil.tradingbot.core.model;

import java.time.Instant;

public record CandleStick(
        Instant timestamp,
        String symbol,
        Long volume,
        Double high,
        Double low,
        Double open,
        Double close
) {}
