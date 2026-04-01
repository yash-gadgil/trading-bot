package io.github.yash_gadgil.tradingbot.core.backtest;

import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.strategy.StrategySignalType;
import io.github.yash_gadgil.tradingbot.testutil.CandleFixtures;
import io.github.yash_gadgil.tradingbot.testutil.FixedHistoricProvider;
import io.github.yash_gadgil.tradingbot.testutil.ScriptedStrategy;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BacktestCostsTest {

    private static final OffsetDateTime START = OffsetDateTime.of(2024, 1, 2, 14, 30, 0, 0, ZoneOffset.UTC);

    private static BacktestResult run(double commission, double slippageBps) {
        List<CandleStick> candles = CandleFixtures.series("AAPL", 100, 110);
        var config = new BacktestConfig(Set.of("AAPL"), START, START.plusDays(1),
                100_000, 10, commission, slippageBps);
        var strategy = new ScriptedStrategy("scripted",
                Arrays.asList(StrategySignalType.ENTER_LONG, StrategySignalType.EXIT));
        return new BacktestEngine(strategy, new FixedHistoricProvider(candles), config).run();
    }

    @Test
    void frictionlessReproducesGrossResult() {
        BacktestResult r = run(0.0, 0.0);
        assertEquals(100_100.0, r.finalEquity(), 1e-6);
        assertEquals(0.0, r.totalCosts(), 1e-9);
    }

    @Test
    void commissionAndSlippageMatchHandComputedValue() {

        BacktestResult r = run(0.01, 10.0);
        assertEquals(100_097.7, r.finalEquity(), 1e-6);
        assertEquals(2.30, r.totalCosts(), 1e-6);
    }
}
