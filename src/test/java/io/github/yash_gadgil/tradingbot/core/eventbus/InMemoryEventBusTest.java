package io.github.yash_gadgil.tradingbot.core.eventbus;

import io.github.yash_gadgil.tradingbot.core.event.TestEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryEventBusTest {

    private EventBus bus;

    @BeforeEach
    void setUp() {
        bus = new InMemoryEventBus();
    }

    @AfterEach
    void tearDown() {
        bus.shutdown();
    }

    @Test
    void handlerReceivesEvent() throws Exception {

        CountDownLatch latch = new CountDownLatch(1);

        bus.subscribe(TestEvent.class, _ -> latch.countDown());
        bus.publish(new TestEvent(0));

        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}
