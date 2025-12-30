package io.github.yash_gadgil.tradingbot.core.strategy;

import io.github.yash_gadgil.tradingbot.core.event.MarketDataEvent;
import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;

import java.util.Optional;

public class FadeTheMoveStrategy implements Strategy {

    @Override
    public Optional<StrategyEvent> onEvent(MarketDataEvent event) {
        return Optional.empty();
    }

}
