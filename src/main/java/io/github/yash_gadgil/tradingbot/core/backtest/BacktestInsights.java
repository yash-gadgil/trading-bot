package io.github.yash_gadgil.tradingbot.core.backtest;

import java.util.List;

public record BacktestInsights(
        double totalReturn,
        double totalPnl,
        double sharpeRatio,
        double sortinoRatio,
        double maxDrawdown,
        double winRate,
        int totalTrades,
        int winningTrades,
        int losingTrades,
        double averageWin,
        double averageLoss,
        double profitFactor,
        double largestWin,
        double largestLoss,
        double totalCosts,
        double benchmarkReturn,
        double alpha
) {

    public static BacktestInsights from(BacktestResult result) {
        List<SimulatedTrade> trades = result.trades();
        double initialCapital = result.initialCapital();
        double finalEquity = result.finalEquity();

        double totalPnl = finalEquity - initialCapital;
        double totalReturn = (totalPnl / initialCapital) * 100.0;

        int totalTrades = trades.size();
        int winningTrades = 0;
        int losingTrades = 0;
        double sumWins = 0;
        double sumLosses = 0;
        double largestWin = 0;
        double largestLoss = 0;

        for (SimulatedTrade t : trades) {
            if (t.pnl() >= 0) {
                winningTrades++;
                sumWins += t.pnl();
                largestWin = Math.max(largestWin, t.pnl());
            } else {
                losingTrades++;
                sumLosses += Math.abs(t.pnl());
                largestLoss = Math.min(largestLoss, t.pnl());
            }
        }

        double winRate = totalTrades > 0 ? (winningTrades / (double) totalTrades) * 100.0 : 0;
        double averageWin = winningTrades > 0 ? sumWins / winningTrades : 0;
        double averageLoss = losingTrades > 0 ? sumLosses / losingTrades : 0;
        double profitFactor = sumLosses > 0 ? sumWins / sumLosses : (sumWins > 0 ? Double.POSITIVE_INFINITY : 0);

        double sharpeRatio = computeSharpe(result.equityCurve(), false);
        double sortinoRatio = computeSharpe(result.equityCurve(), true);
        double maxDrawdown = computeMaxDrawdown(result.equityCurve());

        double benchmarkReturn = curveReturn(result.benchmarkCurve());
        double alpha = totalReturn - benchmarkReturn;

        return new BacktestInsights(
                totalReturn, totalPnl, sharpeRatio, sortinoRatio, maxDrawdown,
                winRate, totalTrades, winningTrades, losingTrades,
                averageWin, averageLoss, profitFactor, largestWin, largestLoss,
                result.totalCosts(), benchmarkReturn, alpha);
    }

    private static double curveReturn(List<EquityPoint> curve) {
        if (curve.size() < 2) return 0;
        double first = curve.getFirst().equity();
        double last = curve.getLast().equity();
        return first == 0 ? 0 : (last - first) / first * 100.0;
    }

    private static double computeSharpe(List<EquityPoint> equityCurve, boolean downsideOnly) {
        if (equityCurve.size() < 2) return 0;

        double[] returns = new double[equityCurve.size() - 1];
        for (int i = 1; i < equityCurve.size(); i++) {
            double prev = equityCurve.get(i - 1).equity();
            double curr = equityCurve.get(i).equity();
            returns[i - 1] = prev == 0 ? 0 : (curr - prev) / prev;
        }

        double mean = 0;
        for (double r : returns) mean += r;
        mean /= returns.length;

        double variance = 0;
        int count = 0;
        for (double r : returns) {
            if (downsideOnly) {
                if (r < 0) { variance += r * r; count++; }
            } else {
                variance += Math.pow(r - mean, 2);
                count++;
            }
        }
        if (count == 0) return 0;
        variance /= count;

        double stdDev = Math.sqrt(variance);
        if (stdDev == 0) return 0;

        double periodsPerYear = 252.0 * 390.0;
        return (mean / stdDev) * Math.sqrt(periodsPerYear);
    }

    private static double computeMaxDrawdown(List<EquityPoint> equityCurve) {
        if (equityCurve.isEmpty()) return 0;

        double peak = equityCurve.getFirst().equity();
        double maxDrawdown = 0;

        for (EquityPoint point : equityCurve) {
            if (point.equity() > peak) peak = point.equity();
            double drawdown = peak == 0 ? 0 : (peak - point.equity()) / peak * 100.0;
            maxDrawdown = Math.max(maxDrawdown, drawdown);
        }
        return maxDrawdown;
    }
}
