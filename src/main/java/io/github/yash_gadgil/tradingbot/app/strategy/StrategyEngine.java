package io.github.yash_gadgil.tradingbot.app.strategy;

import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.TradeEvent;
import io.github.yash_gadgil.tradingbot.core.eventbus.EventBus;
import io.github.yash_gadgil.tradingbot.core.strategy.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Profile("!backtest")
public class StrategyEngine implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(StrategyEngine.class);

    private final EventBus eventBus;
    private final List<Strategy> strategies;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public StrategyEngine(EventBus eventBus, List<Strategy> strategies) {
        this.eventBus = eventBus;
        this.strategies = strategies;
    }

    @Override
    public void start() {
        for (Strategy strategy : strategies) {
            strategy.setEventPublisher(signal -> {
                logger.info("[{}] signal -> {} {}", strategy.id(), signal.signalType(), signal.instrument());
                eventBus.publish(signal);
            });
        }

        eventBus.subscribe(TradeEvent.class, tradeEvent -> logger.debug("{}", tradeEvent));
        eventBus.subscribe(CandleStickEvent.class, candleStickEvent -> {
            logger.debug("{}", candleStickEvent);
            for (Strategy strategy : strategies) {
                strategy.onEvent(candleStickEvent);
            }
        });

        running.set(true);
        logger.info("Strategy Engine started - active strategies: {}",
                strategies.stream().map(Strategy::id).toList());
    }

    @Override
    public void stop() {
        running.set(false);
        logger.info("Strategy Engine stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
