package io.github.yash_gadgil.tradingbot.core.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InMemoryEventBus implements EventBus {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryEventBus.class);

    private final Map<Class<?>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public <E extends Event> void subscribe(Class<E> eventType, EventHandler<? super E> handler) {
        handlers
                .computeIfAbsent(eventType, h -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    @Override
    public void publish(Event event) {
        executor.submit(() -> dispatch(event));
    }

    @SuppressWarnings("unchecked")
    private void dispatch(Event event) {
        List<EventHandler<?>> subs = handlers.get(event.getClass());
        if (subs == null) return;

        for (EventHandler<?> h : subs) {
            try {
                ((EventHandler<Event>) h).handle(event);
            } catch (Exception e) {

                logger.error("Error handling {}", event.getClass().getSimpleName(), e);
            }
        }
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down event bus");
        executor.shutdown();
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }
}
