package io.github.yash_gadgil.tradingbot.core.eventbus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class SynchronousEventBus implements EventBus {

    private final Map<Class<?>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();
    private final Queue<Event> queue = new ArrayDeque<>();
    private boolean dispatching = false;

    @Override
    public <E extends Event> void subscribe(Class<E> eventType, EventHandler<? super E> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @Override
    public void publish(Event event) {
        queue.add(event);
        if (dispatching) return;
        dispatching = true;
        try {
            Event next;
            while ((next = queue.poll()) != null) {
                dispatch(next);
            }
        } finally {
            dispatching = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatch(Event event) {
        List<EventHandler<?>> subs = handlers.get(event.getClass());
        if (subs == null) return;
        for (EventHandler<?> h : List.copyOf(subs)) {
            ((EventHandler<Event>) h).handle(event);
        }
    }

    @Override
    public void shutdown() {

    }
}
