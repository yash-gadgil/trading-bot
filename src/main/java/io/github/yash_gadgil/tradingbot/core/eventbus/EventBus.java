package io.github.yash_gadgil.tradingbot.core.eventbus;

public interface EventBus {

    <E extends Event> void subscribe(
            Class<E> eventType,
            EventHandler<? super E> handler
    );

    void publish(Event event);

    void shutdown();
}
