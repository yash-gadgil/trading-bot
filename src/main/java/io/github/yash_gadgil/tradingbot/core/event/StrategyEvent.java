package io.github.yash_gadgil.tradingbot.core.event;

import io.github.yash_gadgil.tradingbot.core.eventbus.Event;
import io.github.yash_gadgil.tradingbot.core.strategy.StrategySignalType;

public record StrategyEvent(
    String id,
    String instrument,
    StrategySignalType signalType
) implements Event {
}
