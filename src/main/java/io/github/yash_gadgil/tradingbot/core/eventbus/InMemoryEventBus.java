package io.github.yash_gadgil.tradingbot.core.eventbus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InMemoryEventBus implements EventBus {

    private final Map<Class<?>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public <E extends Event> void subscribe(Class<E> eventType, EventHandler<? super E> handler) {
        handlers
                .computeIfAbsent(eventType, _ -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    @Override
    public void publish(Event event) {
        executor.submit(() -> dispatch(event));
    }

    @SuppressWarnings("unchecked")
    private void dispatch(Event event) {
        Class<?> eventType = event.getClass();
        List<EventHandler<?>> subs = handlers.get(eventType);

        if (subs == null) return;

        for (EventHandler<?> h : subs) {
            ( (EventHandler<Event>) h).handle(event);
        }
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
