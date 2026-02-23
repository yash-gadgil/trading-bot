package io.github.yash_gadgil.tradingbot.app.execution;

import io.github.yash_gadgil.tradingbot.core.event.OrderEvent;
import io.github.yash_gadgil.tradingbot.core.event.OrderFillEvent;
import io.github.yash_gadgil.tradingbot.core.eventbus.SynchronousEventBus;
import io.github.yash_gadgil.tradingbot.core.order.IExecutionService;
import io.github.yash_gadgil.tradingbot.core.order.OrderResult;
import io.github.yash_gadgil.tradingbot.core.order.OrderSide;
import io.github.yash_gadgil.tradingbot.core.order.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionServiceTest {

    private static final class FakeBroker implements IExecutionService {
        final List<OrderEvent> placed = new ArrayList<>();
        OrderResult next;
        RuntimeException toThrow;

        @Override
        public OrderResult placeOrder(OrderEvent order) {
            placed.add(order);
            if (toThrow != null) throw toThrow;
            return next;
        }

        @Override
        public boolean cancelOrder(String brokerOrderId) {
            return true;
        }
    }

    private SynchronousEventBus bus;
    private FakeBroker broker;
    private ExecutionService service;
    private List<OrderFillEvent> fills;

    @BeforeEach
    void setUp() {
        bus = new SynchronousEventBus();
        broker = new FakeBroker();
        service = new ExecutionService(bus, broker);
        fills = new ArrayList<>();
        bus.subscribe(OrderFillEvent.class, fills::add);
        service.start();
        assertTrue(service.isRunning());
    }

    private OrderEvent order() {
        return new OrderEvent("cid-1", "strat", "AAPL", OrderSide.BUY, 10,
                OrderType.MARKET, null, Instant.now());
    }

    @Test
    void placesOrderOnceWithSamePayload() {
        broker.next = new OrderResult("cid-1", "brk-1", OrderResult.Status.FILLED, 101.5, 10, "filled");
        bus.publish(order());

        assertEquals(1, broker.placed.size());
        assertEquals("cid-1", broker.placed.getFirst().clientOrderId());
        assertEquals("AAPL", broker.placed.getFirst().instrument());
    }

    @Test
    void filledOrder_publishesFillEventWithBrokerPrice() {
        broker.next = new OrderResult("cid-1", "brk-1", OrderResult.Status.FILLED, 101.5, 10, "filled");
        bus.publish(order());

        assertEquals(1, fills.size());
        OrderFillEvent fill = fills.getFirst();
        assertEquals("AAPL", fill.instrument());
        assertEquals(OrderSide.BUY, fill.side());
        assertEquals(10, fill.quantity());
        assertEquals(101.5, fill.fillPrice());
    }

    @Test
    void failedOrder_publishesNoFillAndDoesNotThrow() {
        broker.next = OrderResult.failed("cid-1", "boom");
        assertDoesNotThrow(() -> bus.publish(order()));
        assertTrue(fills.isEmpty());
    }

    @Test
    void rejectedOrder_publishesNoFill() {
        broker.next = OrderResult.rejected("cid-1", null, "insufficient buying power");
        bus.publish(order());
        assertTrue(fills.isEmpty());
    }

    @Test
    void acceptedWithoutPrice_publishesNoFill() {
        broker.next = new OrderResult("cid-1", "brk-1", OrderResult.Status.ACCEPTED, null, null, "accepted");
        bus.publish(order());
        assertTrue(fills.isEmpty());
    }

    @Test
    void misbehavingBrokerThatThrows_isContained() {
        broker.toThrow = new RuntimeException("network down");
        assertDoesNotThrow(() -> bus.publish(order()));
        assertTrue(fills.isEmpty());
    }
}
