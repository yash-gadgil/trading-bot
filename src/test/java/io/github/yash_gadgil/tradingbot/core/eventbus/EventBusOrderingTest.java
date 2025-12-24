package io.github.yash_gadgil.tradingbot.core.eventbus;

import io.github.yash_gadgil.tradingbot.core.event.TestEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


public class EventBusOrderingTest {

    @Test
    void eventsAreProcessedInPublishOrder() throws Exception {
        EventBus bus = new InMemoryEventBus();

        List<Integer> seen = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch latch = new CountDownLatch(5);

        bus.subscribe(TestEvent.class, e -> {
            seen.add(e.id());
            latch.countDown();
        });

        bus.publish(new TestEvent(1));
        bus.publish(new TestEvent(2));
        bus.publish(new TestEvent(3));
        bus.publish(new TestEvent(4));
        bus.publish(new TestEvent(5));

        assertTrue(latch.await(10, TimeUnit.MILLISECONDS));
        assertIterableEquals(List.of(1, 2, 3, 4, 5), seen);

        bus.shutdown();
    }

}
