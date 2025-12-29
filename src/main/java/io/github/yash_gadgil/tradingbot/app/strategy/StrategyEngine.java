package io.github.yash_gadgil.tradingbot.app.strategy;

import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.TradeEvent;
import io.github.yash_gadgil.tradingbot.core.eventbus.EventBus;
import io.github.yash_gadgil.tradingbot.core.marketdata.HistoricMarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

@Service
public class StrategyEngine implements SmartLifecycle {

    private final HistoricMarketDataProvider historicMarketDataProvider;
    private final EventBus eventBus;

    private static final Logger logger = LoggerFactory.getLogger(StrategyEngine.class);

    @Autowired
    public StrategyEngine(HistoricMarketDataProvider historicMarketDataProvider, EventBus eventBus) {
        this.historicMarketDataProvider = historicMarketDataProvider;
        this.eventBus = eventBus;
    }

    @Override
    public void start() {

        eventBus.subscribe(TradeEvent.class, tradeEvent -> {
            logger.info(tradeEvent.toString());
        });

        eventBus.subscribe(CandleStickEvent.class, candleStickEvent -> {
            logger.info(candleStickEvent.toString());
        });

        logger.info("starting Strategy Engine");
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public int getPhase() {
        return 0;
    }

}
