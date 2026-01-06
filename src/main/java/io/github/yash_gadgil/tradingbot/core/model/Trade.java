package io.github.yash_gadgil.tradingbot.core.model;

public record Trade(
        String symbol,
        Double price,
        Integer size
) {}
