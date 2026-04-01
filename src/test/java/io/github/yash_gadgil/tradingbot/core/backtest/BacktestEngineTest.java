package io.github.yash_gadgil.tradingbot.core.backtest;

import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.strategy.StrategySignalType;
import io.github.yash_gadgil.tradingbot.testutil.CandleFixtures;
import io.github.yash_gadgil.tradingbot.testutil.FixedHistoricProvider;
import io.github.yash_gadgil.tradingbot.testutil.ScriptedStrategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BacktestEngineTest {

    private static final OffsetDateTime START = OffsetDateTime.of(2024, 1, 2, 14, 30, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime END = START.plusDays(1);

    private static BacktestConfig config(Set<String> symbols, double capital, int posSize) {
        return new BacktestConfig(symbols, START, END, capital, posSize);
    }

    private static BacktestResult run(List<CandleStick> candles, Set<String> symbols,
                                      List<StrategySignalType> script, double capital, int posSize) {
        var strategy = new ScriptedStrategy("scripted", script);
        var engine = new BacktestEngine(strategy, new FixedHistoricProvider(candles), config(symbols, capital, posSize));
        return engine.run();
    }

    @Test
    void multiSymbolUnrealizedPnlMarksEachSymbolToItsOwnPrice_D1() {

        List<CandleStick> candles = List.of(
                CandleFixtures.candle("AAPL", CandleFixtures.START, 100),
                CandleFixtures.candle("MSFT", CandleFixtures.START.plus(Duration.ofMinutes(1)), 200),
                CandleFixtures.candle("AAPL", CandleFixtures.START.plus(Duration.ofMinutes(2)), 110),
                CandleFixtures.candle("MSFT", CandleFixtures.START.plus(Duration.ofMinutes(3)), 190));
        var script = Arrays.asList(StrategySignalType.ENTER_LONG, StrategySignalType.ENTER_LONG, null, null);

        BacktestResult r = run(candles, Set.of("AAPL", "MSFT"), script, 100_000, 10);

        assertEquals(97_000.0, r.equityCurve().getLast().equity(), 1e-6);
    }

    @Test
    void forceCloseUsesEachSymbolsOwnLastPrice_D2() {
        List<CandleStick> candles = List.of(
                CandleFixtures.candle("AAPL", CandleFixtures.START, 100),
                CandleFixtures.candle("MSFT", CandleFixtures.START.plus(Duration.ofMinutes(1)), 200),
                CandleFixtures.candle("AAPL", CandleFixtures.START.plus(Duration.ofMinutes(2)), 110),
                CandleFixtures.candle("MSFT", CandleFixtures.START.plus(Duration.ofMinutes(3)), 190));
        var script = Arrays.asList(StrategySignalType.ENTER_LONG, StrategySignalType.ENTER_LONG, null, null);

        BacktestResult r = run(candles, Set.of("AAPL", "MSFT"), script, 100_000, 10);

        var aapl = r.trades().stream().filter(t -> t.symbol().equals("AAPL")).findFirst().orElseThrow();
        var msft = r.trades().stream().filter(t -> t.symbol().equals("MSFT")).findFirst().orElseThrow();
        assertEquals(110.0, aapl.exitPrice(), 1e-9);
        assertEquals(190.0, msft.exitPrice(), 1e-9);
        assertEquals(100_000.0, r.finalEquity(), 1e-6);
    }

    @Test
    void longRoundTripProfit() {
        List<CandleStick> candles = CandleFixtures.series("AAPL", 100, 110);
        var script = Arrays.asList(StrategySignalType.ENTER_LONG, StrategySignalType.EXIT);
        BacktestResult r = run(candles, Set.of("AAPL"), script, 100_000, 10);
        assertEquals(100_100.0, r.finalEquity(), 1e-6);
        assertEquals(1, r.trades().size());
        assertEquals(100.0, r.trades().getFirst().pnl(), 1e-9);
    }

    @Test
    void shortRoundTripProfit() {
        List<CandleStick> candles = CandleFixtures.series("AAPL", 100, 90);
        var script = Arrays.asList(StrategySignalType.ENTER_SHORT, StrategySignalType.EXIT);
        BacktestResult r = run(candles, Set.of("AAPL"), script, 100_000, 10);
        assertEquals(100_100.0, r.finalEquity(), 1e-6);
        assertEquals("SHORT", r.trades().getFirst().side());
    }

    @Test
    void insufficientCashSkipsEntry() {
        List<CandleStick> candles = CandleFixtures.series("AAPL", 100, 110);
        var script = Arrays.asList(StrategySignalType.ENTER_LONG, StrategySignalType.EXIT);
        BacktestResult r = run(candles, Set.of("AAPL"), script, 500, 10);
        assertTrue(r.trades().isEmpty());
        assertEquals(500.0, r.finalEquity(), 1e-9);
    }

    @Test
    void enterShortWhileLongFlipsPosition() {
        List<CandleStick> candles = CandleFixtures.series("AAPL", 100, 105, 95);
        var script = Arrays.asList(
                StrategySignalType.ENTER_LONG, StrategySignalType.ENTER_SHORT, StrategySignalType.EXIT);
        BacktestResult r = run(candles, Set.of("AAPL"), script, 100_000, 10);

        assertEquals(2, r.trades().size());
    }

    @Test
    void holdAndReduceAreNoOpsInSimulator() {
        List<CandleStick> candles = CandleFixtures.series("AAPL", 100, 110, 120);
        var script = Arrays.asList(StrategySignalType.HOLD, StrategySignalType.REDUCE, StrategySignalType.HOLD);
        BacktestResult r = run(candles, Set.of("AAPL"), script, 100_000, 10);
        assertTrue(r.trades().isEmpty());
    }

    @Test
    void emptyDataReturnsCapitalIntact() {
        BacktestResult r = run(List.of(), Set.of("AAPL"), List.of(), 100_000, 10);
        assertEquals(100_000.0, r.finalEquity(), 1e-9);
        assertTrue(r.trades().isEmpty());
    }
}
