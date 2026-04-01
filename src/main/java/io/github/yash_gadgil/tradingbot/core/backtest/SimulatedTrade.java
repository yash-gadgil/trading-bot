package io.github.yash_gadgil.tradingbot.core.backtest;

import java.time.Instant;

public record SimulatedTrade(
        Instant entryTime,
        Instant exitTime,
        String symbol,
        String side,
        double entryPrice,
        double exitPrice,
        int quantity,
        double pnl
) {}
