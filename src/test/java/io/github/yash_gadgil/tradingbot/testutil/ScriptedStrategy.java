package io.github.yash_gadgil.tradingbot.testutil;

import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.MarketDataEvent;
import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;
import io.github.yash_gadgil.tradingbot.core.strategy.Strategy;
import io.github.yash_gadgil.tradingbot.core.strategy.StrategySignalType;

import java.util.List;
import java.util.function.Consumer;

public class ScriptedStrategy implements Strategy {

    private final String id;
    private final List<StrategySignalType> script;
    private int index = 0;
    private Consumer<StrategyEvent> publish;

    public ScriptedStrategy(String id, List<StrategySignalType> script) {
        this.id = id;
        this.script = script;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void onEvent(MarketDataEvent event) {
        StrategySignalType type = index < script.size() ? script.get(index) : null;
        index++;
        if (type != null && publish != null) {
            String symbol = ((CandleStickEvent) event).candleStick().symbol();
            publish.accept(new StrategyEvent(id, symbol, type));
        }
    }

    @Override
    public void setEventPublisher(Consumer<StrategyEvent> eventPublisher) {
        this.publish = eventPublisher;
    }
}
