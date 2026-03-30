package io.github.yash_gadgil.tradingbot.app.backtest;

import io.github.yash_gadgil.tradingbot.testutil.FixedHistoricProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BacktestRunnerArgsTest {

    private BacktestRunner runner() {
        return new BacktestRunner(new FixedHistoricProvider(List.of()), Optional.empty());
    }

    @Test
    void ppoDefaults() {
        var a = BacktestRunner.parseArgs(new String[]{});
        assertEquals("ppo", a.strategyName());
        assertEquals(90, a.days());
        assertEquals(100_000.0, a.capital());
        assertEquals(10, a.positionSize());
        assertEquals(0.0, a.commission());
        assertEquals(0.0, a.slippageBps());
        assertTrue(a.symbols().contains("AAPL"));
    }

    @Test
    void fadeDefaults() {
        var a = BacktestRunner.parseArgs(new String[]{"--strategy=fade"});
        assertEquals("fade", a.strategyName());
        assertEquals(30, a.days());
    }

    @Test
    void overridesAllArgs() {
        var a = BacktestRunner.parseArgs(new String[]{
                "--strategy=fade", "--symbols=AAPL,TSLA", "--days=45",
                "--capital=50000", "--position-size=5", "--commission=0.01", "--slippage-bps=10"});
        assertEquals(Set.of("AAPL", "TSLA"), a.symbols());
        assertEquals(45, a.days());
        assertEquals(50_000.0, a.capital());
        assertEquals(5, a.positionSize());
        assertEquals(0.01, a.commission());
        assertEquals(10.0, a.slippageBps());
    }

    @Test
    void unknownStrategyThrowsClearError() {
        var ex = assertThrows(IllegalArgumentException.class, () -> runner().buildStrategy("nope"));
        assertTrue(ex.getMessage().contains("nope"));
    }

    @Test
    void fadeStrategyBuilds() {
        assertEquals("fade-the-move", runner().buildStrategy("fade").id());
    }
}
