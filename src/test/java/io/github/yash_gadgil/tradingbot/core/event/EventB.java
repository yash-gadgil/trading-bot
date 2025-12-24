package io.github.yash_gadgil.tradingbot.core.event;

import io.github.yash_gadgil.tradingbot.core.eventbus.Event;

import java.time.Instant;

public record EventB(Instant timestamp) implements Event {
    public EventB() {
        this(Instant.now());
    }
}
