package io.github.yash_gadgil.tradingbot.core.backtest;

import java.time.OffsetDateTime;
import java.util.Set;

public record BacktestConfig(
        Set<String> symbols,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        double initialCapital,
        int positionSizeShares,
        double commissionPerShare,
        double slippageBps
) {

    public BacktestConfig(Set<String> symbols, OffsetDateTime startTime, OffsetDateTime endTime,
                          double initialCapital, int positionSizeShares) {
        this(symbols, startTime, endTime, initialCapital, positionSizeShares, 0.0, 0.0);
    }
}
