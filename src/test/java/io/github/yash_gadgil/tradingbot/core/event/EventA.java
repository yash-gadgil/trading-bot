package io.github.yash_gadgil.tradingbot.core.event;

import io.github.yash_gadgil.tradingbot.core.eventbus.Event;

import java.time.Instant;

public record EventA(Instant timestamp) implements Event {
    public EventA() {
        this(Instant.now());
    }
}
