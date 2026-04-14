package io.github.yash_gadgil.tradingbot.app.risk;

import io.github.yash_gadgil.tradingbot.core.account.HeldPosition;
import io.github.yash_gadgil.tradingbot.core.order.OrderSide;
import io.github.yash_gadgil.tradingbot.core.strategy.StrategySignalType;
import io.github.yash_gadgil.tradingbot.testutil.TradeLoopHarness;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KillSwitchTest {

    private TradeLoopHarness trippedHarness(boolean flatten) {
        TradeLoopHarness h = new TradeLoopHarness();
        h.props.setDailyLossLimit(100.0);
        h.props.setFlattenOnKill(flatten);
        h.openPositions.add(new HeldPosition("AAPL", 100, 100.0));
        h.start();
        h.price("AAPL", 98.0);
        return h;
    }

    @Test
    void breachTripsKillSwitch() {
        TradeLoopHarness h = trippedHarness(false);
        assertTrue(h.risk.isKillSwitchTripped());
    }

    @Test
    void trippedSwitchBlocksNewEntries() {
        TradeLoopHarness h = trippedHarness(false);
        h.orders.clear();
        h.price("MSFT", 50.0);
        h.signal("MSFT", StrategySignalType.ENTER_LONG);
        assertTrue(h.orders.isEmpty(), "no new entries once the kill switch is tripped");
    }

    @Test
    void exitsStillAllowedAfterTrip() {
        TradeLoopHarness h = trippedHarness(false);
        h.orders.clear();
        h.signal("AAPL", StrategySignalType.EXIT);
        assertEquals(1, h.orders.size());
        assertEquals(OrderSide.SELL, h.orders.getFirst().side());
        assertEquals(100, h.orders.getFirst().quantity());
    }

    @Test
    void flattenOnKill_emitsExitOrdersImmediately() {
        TradeLoopHarness h = trippedHarness(true);
        assertTrue(h.risk.isKillSwitchTripped());
        assertTrue(h.orders.stream().anyMatch(o ->
                o.instrument().equals("AAPL") && o.side() == OrderSide.SELL && o.quantity() == 100));
        assertEquals(0, h.book.quantityOf("AAPL"), "flatten should close the position");
    }

    @Test
    void notTrippedWhileWithinLimit() {
        TradeLoopHarness h = new TradeLoopHarness();
        h.props.setDailyLossLimit(1_000.0);
        h.openPositions.add(new HeldPosition("AAPL", 100, 100.0));
        h.start();
        h.price("AAPL", 98.0);
        assertFalse(h.risk.isKillSwitchTripped());
    }
}
