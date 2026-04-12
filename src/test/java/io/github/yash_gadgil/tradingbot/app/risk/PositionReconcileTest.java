package io.github.yash_gadgil.tradingbot.app.risk;

import io.github.yash_gadgil.tradingbot.core.account.HeldPosition;
import io.github.yash_gadgil.tradingbot.core.strategy.StrategySignalType;
import io.github.yash_gadgil.tradingbot.testutil.TradeLoopHarness;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionReconcileTest {

    @Test
    void bookIsSeededFromBrokerBeforeFirstSignal() {
        TradeLoopHarness h = new TradeLoopHarness();
        h.openPositions.add(new HeldPosition("AAPL", 10, 150.0));
        h.start();

        assertEquals(10, h.book.quantityOf("AAPL"));
        assertEquals(150.0, h.book.get("AAPL").orElseThrow().avgEntryPrice(), 1e-9);
    }

    @Test
    void reconciledPositionPreventsDoubleEntryOnRestart() {
        TradeLoopHarness h = new TradeLoopHarness();
        h.openPositions.add(new HeldPosition("AAPL", 10, 150.0));
        h.start();

        h.price("AAPL", 150.0);
        h.signal("AAPL", StrategySignalType.ENTER_LONG);

        assertTrue(h.orders.isEmpty(), "should not re-enter a reconciled position");
    }
}
