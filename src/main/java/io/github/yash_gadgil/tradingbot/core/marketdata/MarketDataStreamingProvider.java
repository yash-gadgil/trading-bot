package io.github.yash_gadgil.tradingbot.core.marketdata;

import io.github.yash_gadgil.tradingbot.core.event.MarketDataEvent;

import java.util.function.Consumer;

public interface MarketDataStreamingProvider {

    void connect();

    void disconnect();

    void setEventPublisher(Consumer<MarketDataEvent> eventPublisher);
}
