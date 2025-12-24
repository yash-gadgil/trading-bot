package io.github.yash_gadgil.tradingbot.core.eventbus;

@FunctionalInterface
public interface EventHandler<E extends Event> {
    void handle(E event);
}
