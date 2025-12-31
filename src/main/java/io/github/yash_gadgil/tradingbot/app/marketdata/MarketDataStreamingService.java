package io.github.yash_gadgil.tradingbot.app.marketdata;

import io.github.yash_gadgil.tradingbot.core.eventbus.EventBus;
import io.github.yash_gadgil.tradingbot.core.marketdata.MarketDataStreamingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

@Service
public class MarketDataStreamingService implements SmartLifecycle {

    private final MarketDataStreamingProvider marketDataStreamingService;
    private static final Logger logger = LoggerFactory.getLogger(MarketDataStreamingService.class);

    private final EventBus eventBus;

    @Autowired
    public MarketDataStreamingService(MarketDataStreamingProvider marketDataStreamingServiceInterface, EventBus eventBus) {
        this.marketDataStreamingService = marketDataStreamingServiceInterface;
        this.eventBus = eventBus;
    }


    @Override
    public void start() {
        marketDataStreamingService.setEventPublisher(eventBus::publish);

        marketDataStreamingService.connect();
        logger.info("Starting MarketData Streaming Service");
    }

    @Override
    public void stop() {
        marketDataStreamingService.disconnect();
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public int getPhase() {
        return 100;
    }
}
