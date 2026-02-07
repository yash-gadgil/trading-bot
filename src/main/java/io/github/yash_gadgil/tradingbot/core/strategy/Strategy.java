package io.github.yash_gadgil.tradingbot.core.strategy;

import io.github.yash_gadgil.tradingbot.core.event.MarketDataEvent;
import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;

import java.util.function.Consumer;

public interface Strategy {

    String id();

    void onEvent(MarketDataEvent event);

    void setEventPublisher(Consumer<StrategyEvent> eventPublisher);
}
