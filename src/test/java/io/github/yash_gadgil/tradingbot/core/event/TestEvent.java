package io.github.yash_gadgil.tradingbot.core.event;

import io.github.yash_gadgil.tradingbot.core.eventbus.Event;

import java.time.Instant;

public record TestEvent(Instant timestamp, int id) implements Event {
    public TestEvent(int id) {
        this(Instant.now(), id);
    }
}
