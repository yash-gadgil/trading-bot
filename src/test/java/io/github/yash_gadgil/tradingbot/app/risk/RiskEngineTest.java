package io.github.yash_gadgil.tradingbot.app.risk;

import io.github.yash_gadgil.tradingbot.core.event.OrderEvent;
import io.github.yash_gadgil.tradingbot.core.order.OrderSide;
import io.github.yash_gadgil.tradingbot.core.order.OrderType;
import io.github.yash_gadgil.tradingbot.core.strategy.StrategySignalType;
import io.github.yash_gadgil.tradingbot.testutil.TradeLoopHarness;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskEngineTest {

    @Test
    void enterLongWhileFlat_placesBuyWithExpectedQty() {
        TradeLoopHarness h = new TradeLoopHarness().start();
        h.price("AAPL", 100.0);
        h.signal("AAPL", StrategySignalType.ENTER_LONG);

        assertEquals(1, h.orders.size());
        OrderEvent o = h.orders.getFirst();
        assertEquals(OrderSide.BUY, o.side());
        assertEquals("AAPL", o.instrument());
        assertEquals(10, o.quantity());
        assertEquals(OrderType.MARKET, o.orderType());
        assertNotNull(o.clientOrderId());
        assertEquals(10, h.book.quantityOf("AAPL"));
    }

    @Test
    void enterLongWhileAlreadyLong_isDeduped() {
        TradeLoopHarness h = new TradeLoopHarness().start();
        h.price("AAPL", 100.0);
        h.signal("AAPL", StrategySignalType.ENTER_LONG);
        h.signal("AAPL", StrategySignalType.ENTER_LONG);

        assertEquals(1, h.orders.size());
    }

    @Test
    void enterShortWhileFlat_placesSell() {
        TradeLoopHarness h = new TradeLoopHarness().start();
        h.price("MSFT", 200.0);
        h.signal("MSFT", StrategySignalType.ENTER_SHORT);

        assertEquals(1, h.orders.size());
        assertEquals(OrderSide.SELL, h.orders.getFirst().side());
        assertEquals(5, h.orders.getFirst().quantity());
        assertEquals(-5, h.book.quantityOf("MSFT"));
    }

    @Test
    void exitWhileLong_sellsHeldQuantity() {
        TradeLoopHarness h = new TradeLoopHarness().start();
        h.price("AAPL", 100.0);
        h.signal("AAPL", StrategySignalType.ENTER_LONG);
        h.signal("AAPL", StrategySignalType.EXIT);

        assertEquals(2, h.orders.size());
        OrderEvent exit = h.orders.get(1);
        assertEquals(OrderSide.SELL, exit.side());
        assertEquals(10, exit.quantity());
        assertEquals(0, h.book.quantityOf("AAPL"));
    }

    @Test
    void exitWhileFlat_placesNoOrder() {
        TradeLoopHarness h = new TradeLoopHarness().start();
        h.price("AAPL", 100.0);
        h.signal("AAPL", StrategySignalType.EXIT);
        assertTrue(h.orders.isEmpty());
    }

    @Test
    void reduceWhileLong_halvesPosition() {
        TradeLoopHarness h = new TradeLoopHarness().start();
        h.price("AAPL", 100.0);
        h.signal("AAPL", StrategySignalType.ENTER_LONG);
        h.signal("AAPL", StrategySignalType.REDUCE);

        assertEquals(2, h.orders.size());
        OrderEvent reduce = h.orders.get(1);
        assertEquals(OrderSide.SELL, reduce.side());
        assertEquals(5, reduce.quantity());
        assertEquals(5, h.book.quantityOf("AAPL"));
    }

    @Test
    void sizingFollowsAccountSnapshotAndPrice() {
        TradeLoopHarness h = new TradeLoopHarness();
        h.balance = 50_000.0;
        h.start();
        h.price("TSLA", 250.0);
        h.signal("TSLA", StrategySignalType.ENTER_LONG);

        assertEquals(1, h.orders.size());
        assertEquals(2, h.orders.getFirst().quantity());
    }

    @Test
    void noPriceYet_skipsOrder() {
        TradeLoopHarness h = new TradeLoopHarness().start();
        h.signal("NVDA", StrategySignalType.ENTER_LONG);
        assertTrue(h.orders.isEmpty());
        assertEquals(0, h.book.quantityOf("NVDA"));
    }

    @Test
    void holdSignal_doesNothing() {
        TradeLoopHarness h = new TradeLoopHarness().start();
        h.price("AAPL", 100.0);
        h.signal("AAPL", StrategySignalType.HOLD);
        assertTrue(h.orders.isEmpty());
    }
}
