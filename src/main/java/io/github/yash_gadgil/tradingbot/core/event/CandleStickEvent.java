package io.github.yash_gadgil.tradingbot.core.event;

import io.github.yash_gadgil.tradingbot.core.model.CandleStick;

import java.time.Instant;

public record CandleStickEvent(
       Instant timestamp,
       CandleStick candleStick
) implements MarketDataEvent {}
