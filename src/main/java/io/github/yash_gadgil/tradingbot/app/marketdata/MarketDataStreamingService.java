package io.github.yash_gadgil.tradingbot.app.marketdata;

import io.github.yash_gadgil.tradingbot.core.eventbus.EventBus;
import io.github.yash_gadgil.tradingbot.core.marketdata.MarketDataStreamingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Profile("!backtest")
public class MarketDataStreamingService implements SmartLifecycle {

    private final MarketDataStreamingProvider marketDataStreamingService;
    private static final Logger logger = LoggerFactory.getLogger(MarketDataStreamingService.class);

    private final EventBus eventBus;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    public MarketDataStreamingService(MarketDataStreamingProvider marketDataStreamingServiceInterface, EventBus eventBus) {
        this.marketDataStreamingService = marketDataStreamingServiceInterface;
        this.eventBus = eventBus;
    }

    @Override
    public void start() {
        marketDataStreamingService.setEventPublisher(eventBus::publish);
        marketDataStreamingService.connect();
        running.set(true);
        logger.info("MarketData Streaming Service started");
    }

    @Override
    public void stop() {
        marketDataStreamingService.disconnect();
        running.set(false);
        logger.info("MarketData Streaming Service stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 100;
    }
}
