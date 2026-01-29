package io.github.yash_gadgil.tradingbot.core.account;

public record AccountSnapshot(
        String currency,
        double totalBalance,
        int multiplier
) {
}
