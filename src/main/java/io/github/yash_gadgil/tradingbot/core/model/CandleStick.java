package io.github.yash_gadgil.tradingbot.core.model;

public record CandleStick(
        String symbol,
        Long volume,
        Double high,
        Double low,
        Double open,
        Double close
) {}
