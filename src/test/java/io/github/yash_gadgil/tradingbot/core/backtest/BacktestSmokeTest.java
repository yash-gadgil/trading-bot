package io.github.yash_gadgil.tradingbot.core.backtest;

import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.strategy.FadeTheMoveStrategy;
import io.github.yash_gadgil.tradingbot.testutil.CandleFixtures;
import io.github.yash_gadgil.tradingbot.testutil.FixedHistoricProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BacktestSmokeTest {

    private static List<CandleStick> oscillatingSeries() {
        java.util.List<Double> closes = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) closes.add(100.0);
        for (int c = 0; c < 12; c++) {
            closes.add(112.0);
            closes.add(100.0);
            closes.add(88.0);
            closes.add(100.0);
        }
        double[] arr = closes.stream().mapToDouble(Double::doubleValue).toArray();
        return CandleFixtures.series("AAPL", arr);
    }

    @Test
    void runsEndToEndWithFiniteInsights() {
        var config = new BacktestConfig(Set.of("AAPL"),
                java.time.OffsetDateTime.parse("2024-01-02T14:30:00Z"),
                java.time.OffsetDateTime.parse("2024-01-03T14:30:00Z"), 100_000, 10);
        var engine = new BacktestEngine(new FadeTheMoveStrategy(14, 2.0),
                new FixedHistoricProvider(oscillatingSeries()), config);

        BacktestResult result = engine.run();
        BacktestInsights insights = BacktestInsights.from(result);

        assertFalse(Double.isNaN(insights.totalReturn()));
        assertFalse(Double.isNaN(insights.sharpeRatio()));
        assertFalse(Double.isNaN(insights.sortinoRatio()));
        assertFalse(Double.isNaN(insights.maxDrawdown()));
        assertFalse(Double.isNaN(insights.benchmarkReturn()));
        assertFalse(Double.isNaN(insights.alpha()));
        assertTrue(Double.isFinite(insights.maxDrawdown()));
        assertTrue(insights.totalTrades() > 0, "oscillating series should produce trades");
    }

    @Test
    void writesCsvResults() throws Exception {
        var config = new BacktestConfig(Set.of("AAPL"),
                java.time.OffsetDateTime.parse("2024-01-02T14:30:00Z"),
                java.time.OffsetDateTime.parse("2024-01-03T14:30:00Z"), 100_000, 10);
        var engine = new BacktestEngine(new FadeTheMoveStrategy(14, 2.0),
                new FixedHistoricProvider(oscillatingSeries()), config);
        BacktestResult result = engine.run();

        Path dir = Files.createTempDirectory("backtest-results");
        try {
            BacktestCsvWriter.write(result, dir);
            String equity = Files.readString(dir.resolve("equity.csv"));
            String trades = Files.readString(dir.resolve("trades.csv"));
            assertTrue(equity.startsWith("timestamp,equity,benchmark"));
            assertTrue(trades.startsWith("entry_time,exit_time,symbol,side"));
            assertTrue(equity.lines().count() > 1, "equity curve should have rows");
            assertTrue(trades.lines().count() > 1, "oscillating series should produce trades");
        } finally {
            Files.deleteIfExists(dir.resolve("equity.csv"));
            Files.deleteIfExists(dir.resolve("trades.csv"));
            Files.deleteIfExists(dir);
        }
    }
}
