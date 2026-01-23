package io.github.yash_gadgil.tradingbot.core.strategy;

import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.MarketDataEvent;
import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FadeTheMoveStrategy implements Strategy {

    private Consumer<StrategyEvent> publishEvent;

    private final int window;
    private final double zThreshold;
    private final Map<String, Deque<Double>> instrumentPrices = new HashMap<>();

    public FadeTheMoveStrategy(int window, double zThreshold) {
        this.window = window;
        this.zThreshold = zThreshold;
    }

    @Override
    public String id() {
        return "fade-the-move";
    }

    @Override
    public void onEvent(MarketDataEvent event) {

        CandleStick candleStick = ((CandleStickEvent) event).candleStick();

        if (!instrumentPrices.containsKey(candleStick.symbol())) {
            instrumentPrices.put(candleStick.symbol(), new ArrayDeque<>());
        }

        Deque<Double> prices = instrumentPrices.get(candleStick.symbol());

        prices.addLast(candleStick.close());

        if (prices.size() < window) return;
        if (prices.size() > window) prices.removeFirst();

        double mean = prices.stream().mapToDouble(d -> d).average().orElse(0);
        double std = Math.sqrt(
                prices.stream()
                        .mapToDouble(p -> Math.pow(p - mean, 2))
                        .average()
                        .orElse(0)
        );

        double z = (candleStick.close() - mean) / std;

        if (z > zThreshold) {
            publishEvent.accept(new StrategyEvent(
                    id(),
                    candleStick.symbol(),
                    StrategySignalType.ENTER_SHORT
            ));
            return;
        }

        if (z < -zThreshold) {
            publishEvent.accept(new StrategyEvent(
                    id(),
                    candleStick.symbol(),
                    StrategySignalType.ENTER_LONG
            ));
        }

    }

    @Override
    public void setEventPublisher(Consumer<StrategyEvent> eventPublisher) {
        publishEvent = eventPublisher;
    }

}
