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

    private static final double STD_EPS = 1e-8;

    private Consumer<StrategyEvent> publishEvent;

    private final int window;
    private final double zThreshold;
    private final Map<String, Deque<Double>> instrumentPrices = new HashMap<>();

    private final Map<String, Integer> state = new HashMap<>();

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
        String symbol = candleStick.symbol();

        Deque<Double> prices = instrumentPrices.computeIfAbsent(symbol, k -> new ArrayDeque<>());
        prices.addLast(candleStick.close());

        if (prices.size() < window) return;
        if (prices.size() > window) prices.removeFirst();

        double mean = prices.stream().mapToDouble(d -> d).average().orElse(0);
        double std = Math.sqrt(prices.stream().mapToDouble(p -> Math.pow(p - mean, 2)).average().orElse(0));

        if (std < STD_EPS) return;

        double z = (candleStick.close() - mean) / std;
        int pos = state.getOrDefault(symbol, 0);

        if (pos == 0) {

            if (z > zThreshold) {
                emit(symbol, StrategySignalType.ENTER_SHORT);
                state.put(symbol, -1);
            } else if (z < -zThreshold) {
                emit(symbol, StrategySignalType.ENTER_LONG);
                state.put(symbol, 1);
            }
        } else if (pos == -1) {

            if (z <= 0) {
                emit(symbol, StrategySignalType.EXIT);
                state.put(symbol, 0);
            }
        } else {

            if (z >= 0) {
                emit(symbol, StrategySignalType.EXIT);
                state.put(symbol, 0);
            }
        }
    }

    private void emit(String symbol, StrategySignalType type) {
        if (publishEvent != null) {
            publishEvent.accept(new StrategyEvent(id(), symbol, type));
        }
    }

    @Override
    public void setEventPublisher(Consumer<StrategyEvent> eventPublisher) {
        publishEvent = eventPublisher;
    }
}
