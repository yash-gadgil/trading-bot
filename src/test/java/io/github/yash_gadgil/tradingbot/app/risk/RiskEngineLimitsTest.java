package io.github.yash_gadgil.tradingbot.app.risk;

import io.github.yash_gadgil.tradingbot.core.strategy.StrategySignalType;
import io.github.yash_gadgil.tradingbot.testutil.TradeLoopHarness;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskEngineLimitsTest {

    @Test
    void perSymbolCapBlocksEntry() {
        TradeLoopHarness h = new TradeLoopHarness();
        h.props.setMaxPositionPerSymbol(5);
        h.start();
        h.price("AAPL", 100.0);
        h.signal("AAPL", StrategySignalType.ENTER_LONG);
        assertTrue(h.orders.isEmpty(), "per-symbol cap should block the order");
    }

    @Test
    void grossExposureCapBlocksEntry() {
        TradeLoopHarness h = new TradeLoopHarness();
        h.props.setMaxGrossExposure(0.001);
        h.start();
        h.price("AAPL", 100.0);
        h.signal("AAPL", StrategySignalType.ENTER_LONG);
        assertTrue(h.orders.isEmpty(), "gross exposure cap should block the order");
    }

    @Test
    void buyingPowerBlocksEntry() {
        TradeLoopHarness h = new TradeLoopHarness();
        h.balance = 1_000.0;
        h.props.setPerOrderFraction(2.0);
        h.props.setMaxGrossExposure(100.0);
        h.props.setMaxPositionPerSymbol(100_000);
        h.start();
        h.price("AAPL", 100.0);
        h.signal("AAPL", StrategySignalType.ENTER_LONG);
        assertTrue(h.orders.isEmpty(), "buying-power check should block the order");
    }

    @Test
    void withinAllLimits_orderGoesThrough() {
        TradeLoopHarness h = new TradeLoopHarness().start();
        h.price("AAPL", 100.0);
        h.signal("AAPL", StrategySignalType.ENTER_LONG);
        assertTrue(!h.orders.isEmpty(), "a compliant order should be placed");
    }
}
