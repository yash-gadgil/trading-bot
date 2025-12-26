package io.github.yash_gadgil.tradingbot.core.event;

public record TradeEvent(
        String symbol,
        Double price,
        Integer size
) implements MarketDataEvent {}

