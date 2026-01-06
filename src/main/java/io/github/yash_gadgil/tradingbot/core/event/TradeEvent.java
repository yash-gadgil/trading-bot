package io.github.yash_gadgil.tradingbot.core.event;

import io.github.yash_gadgil.tradingbot.core.model.Trade;

import java.time.Instant;

public record TradeEvent(
        Instant timestamp,
        Trade trade
) implements MarketDataEvent {}

