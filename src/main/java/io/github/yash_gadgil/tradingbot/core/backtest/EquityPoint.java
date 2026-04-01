package io.github.yash_gadgil.tradingbot.core.backtest;

import java.time.Instant;

public record EquityPoint(
        Instant timestamp,
        double equity
) {}
