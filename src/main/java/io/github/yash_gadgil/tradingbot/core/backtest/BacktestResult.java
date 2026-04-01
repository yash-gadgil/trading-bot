package io.github.yash_gadgil.tradingbot.core.backtest;

import java.util.List;

public record BacktestResult(
        String strategyName,
        BacktestConfig config,
        List<SimulatedTrade> trades,
        List<EquityPoint> equityCurve,
        List<EquityPoint> benchmarkCurve,
        double initialCapital,
        double finalEquity,
        int totalBarsProcessed,
        int totalSignals,
        double totalCosts
) {}
