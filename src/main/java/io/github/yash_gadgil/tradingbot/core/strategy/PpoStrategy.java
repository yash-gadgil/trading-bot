package io.github.yash_gadgil.tradingbot.core.strategy;

import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.MarketDataEvent;
import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.providers.onnx.OnnxPpoAgent;
import io.github.yash_gadgil.tradingbot.providers.onnx.PpoAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PpoStrategy implements Strategy {

    private static final Logger logger = LoggerFactory.getLogger(PpoStrategy.class);

    private static final int CACHE_SIZE = 110;

    private Consumer<StrategyEvent> publishEvent;

    private final Map<String, Deque<CandleStick>> instrumentPrices = new HashMap<>();

    private final Map<String, Integer> logicalPositions = new HashMap<>();

    private final PpoAgent agent;
    private final double confidenceThreshold;

    public PpoStrategy() {
        this(new OnnxPpoAgent(), 0.0);
    }

    public PpoStrategy(double confidenceThreshold) {
        this(new OnnxPpoAgent(), confidenceThreshold);
    }

    public PpoStrategy(PpoAgent agent) {
        this(agent, 0.0);
    }

    public PpoStrategy(PpoAgent agent, double confidenceThreshold) {
        this.agent = agent;
        this.confidenceThreshold = confidenceThreshold;
    }

    public PpoAgent agent() {
        return agent;
    }

    @Override
    public String id() {
        return "ppo-rl-agent";
    }

    @Override
    public void onEvent(MarketDataEvent event) {
        if (!(event instanceof CandleStickEvent candleEvent)) {
            return;
        }

        CandleStick candle = candleEvent.candleStick();
        String symbol = candle.symbol();

        Deque<CandleStick> history = instrumentPrices.computeIfAbsent(symbol, k -> new ArrayDeque<>());
        logicalPositions.putIfAbsent(symbol, 0);

        history.addLast(candle);
        if (history.size() > CACHE_SIZE) {
            history.removeFirst();
        }
        if (history.size() < CACHE_SIZE) {
            return;
        }

        int position = logicalPositions.get(symbol);
        PpoAgent.Prediction prediction;
        try {
            prediction = agent.predictWithConfidence(history, position);
        } catch (RuntimeException e) {
            logger.warn("PPO inference failed for {}; treating as HOLD", symbol, e);
            return;
        }

        StrategySignalType signal = switch (prediction.action()) {
            case 0 -> StrategySignalType.ENTER_LONG;
            case 1 -> StrategySignalType.ENTER_SHORT;
            case 2 -> StrategySignalType.EXIT;
            case 4 -> StrategySignalType.REDUCE;
            default -> StrategySignalType.HOLD;
        };

        if (signal == StrategySignalType.HOLD) {
            return;
        }
        if (prediction.confidence() < confidenceThreshold) {
            logger.debug("Gating {} {} - confidence {} < threshold {}",
                    signal, symbol, prediction.confidence(), confidenceThreshold);
            return;
        }

        switch (prediction.action()) {
            case 0 -> logicalPositions.put(symbol, 1);
            case 1 -> logicalPositions.put(symbol, -1);
            case 2 -> logicalPositions.put(symbol, 0);
            default -> {  }
        }

        if (publishEvent != null) {
            publishEvent.accept(new StrategyEvent(id(), symbol, signal, (double) prediction.confidence()));
        }
    }

    @Override
    public void setEventPublisher(Consumer<StrategyEvent> eventPublisher) {
        this.publishEvent = eventPublisher;
    }
}
