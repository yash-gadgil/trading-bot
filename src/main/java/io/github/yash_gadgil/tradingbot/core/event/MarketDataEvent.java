package io.github.yash_gadgil.tradingbot.core.event;

import io.github.yash_gadgil.tradingbot.core.eventbus.Event;

import java.time.Instant;

public interface MarketDataEvent extends Event {

    Instant timestamp();

}
