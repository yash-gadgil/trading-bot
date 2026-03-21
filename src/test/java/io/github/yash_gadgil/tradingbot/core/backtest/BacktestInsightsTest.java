package io.github.yash_gadgil.tradingbot.core.backtest;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BacktestInsightsTest {

    private static EquityPoint pt(int minute, double equity) {
        return new EquityPoint(Instant.parse("2024-01-02T14:30:00Z").plusSeconds(minute * 60L), equity);
    }

    private static SimulatedTrade trade(String sym, double pnl) {
        Instant t = Instant.parse("2024-01-02T14:30:00Z");
        return new SimulatedTrade(t, t.plusSeconds(60), sym, "LONG", 100.0, 100.0 + pnl / 10.0, 10, pnl);
    }

    private BacktestResult result() {
        var config = new BacktestConfig(Set.of("AAPL"),
                OffsetDateTime.of(2024, 1, 2, 14, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 3, 14, 30, 0, 0, ZoneOffset.UTC), 100_000, 10);
        List<EquityPoint> equity = List.of(pt(0, 100_000), pt(1, 101_000), pt(2, 100_500), pt(3, 102_000));
        List<EquityPoint> benchmark = List.of(pt(0, 100_000), pt(1, 100_400), pt(2, 100_700), pt(3, 101_000));
        List<SimulatedTrade> trades = List.of(trade("AAPL", 1_000), trade("AAPL", -500));
        return new BacktestResult("scripted", config, trades, equity, benchmark,
                100_000, 102_000, 4, 2, 12.5);
    }

    @Test
    void coreReturnsAndTradeStats() {
        BacktestInsights i = BacktestInsights.from(result());
        assertEquals(2.0, i.totalReturn(), 1e-9);
        assertEquals(2_000.0, i.totalPnl(), 1e-9);
        assertEquals(2, i.totalTrades());
        assertEquals(1, i.winningTrades());
        assertEquals(1, i.losingTrades());
        assertEquals(50.0, i.winRate(), 1e-9);
        assertEquals(2.0, i.profitFactor(), 1e-9);
        assertEquals(1_000.0, i.averageWin(), 1e-9);
        assertEquals(500.0, i.averageLoss(), 1e-9);
        assertEquals(1_000.0, i.largestWin(), 1e-9);
        assertEquals(-500.0, i.largestLoss(), 1e-9);
    }

    @Test
    void benchmarkAndAlpha() {
        BacktestInsights i = BacktestInsights.from(result());
        assertEquals(1.0, i.benchmarkReturn(), 1e-9);
        assertEquals(1.0, i.alpha(), 1e-9);
        assertEquals(12.5, i.totalCosts(), 1e-9);
    }

    @Test
    void maxDrawdownFromEquityCurve() {
        BacktestInsights i = BacktestInsights.from(result());

        assertEquals(500.0 / 101_000.0 * 100.0, i.maxDrawdown(), 1e-9);
    }

    @Test
    void riskRatiosFiniteAndPositiveForProfitableCurve() {
        BacktestInsights i = BacktestInsights.from(result());
        assertTrue(Double.isFinite(i.sharpeRatio()) && i.sharpeRatio() > 0);
        assertTrue(Double.isFinite(i.sortinoRatio()) && i.sortinoRatio() > 0);
    }

    @Test
    void emptyTradesProduceNoNaN() {
        var config = new BacktestConfig(Set.of("AAPL"),
                OffsetDateTime.of(2024, 1, 2, 14, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 3, 14, 30, 0, 0, ZoneOffset.UTC), 100_000, 10);
        BacktestResult empty = new BacktestResult("scripted", config, List.of(),
                List.of(pt(0, 100_000)), List.of(pt(0, 100_000)), 100_000, 100_000, 1, 0, 0.0);
        BacktestInsights i = BacktestInsights.from(empty);
        assertEquals(0.0, i.winRate(), 1e-9);
        assertFalse(Double.isNaN(i.sharpeRatio()));
        assertFalse(Double.isNaN(i.maxDrawdown()));
    }
}
