package io.github.yash_gadgil.tradingbot.core.eventbus;

import io.github.yash_gadgil.tradingbot.core.event.EventA;
import io.github.yash_gadgil.tradingbot.core.event.EventB;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventBusReentryTest {

    @Test
    void eventBusReentryTest() throws Exception {

        EventBus bus = new InMemoryEventBus();
        List<String> seen = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);


        bus.subscribe(EventA.class, _ -> {
            seen.add("A");
            bus.publish(new EventB());
            latch.countDown();
        });

        bus.subscribe(EventB.class, _ -> {
            seen.add("B");
            latch.countDown();
        });

        bus.publish(new EventA());

        assertTrue(latch.await(50, TimeUnit.MILLISECONDS));
        assertEquals(List.of("A", "B"), seen);

        bus.shutdown();
    }
}
