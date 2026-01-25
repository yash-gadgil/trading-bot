package io.github.yash_gadgil.tradingbot.core.marketdata;

import java.util.function.DoubleSupplier;

public final class ReconnectPolicy {

    private final long baseMs;
    private final long maxMs;
    private final double multiplier;
    private final double jitterFraction;
    private final DoubleSupplier jitterRng;

    private int attempt = 0;

    public ReconnectPolicy(long baseMs, long maxMs, double multiplier,
                           double jitterFraction, DoubleSupplier jitterRng) {
        this.baseMs = baseMs;
        this.maxMs = maxMs;
        this.multiplier = multiplier;
        this.jitterFraction = jitterFraction;
        this.jitterRng = jitterRng;
    }

    public ReconnectPolicy(long baseMs, long maxMs, double multiplier) {
        this(baseMs, maxMs, multiplier, 0.2, Math::random);
    }

    public long baseDelayMs(int attempt) {
        double exp = baseMs * Math.pow(multiplier, attempt);
        return (long) Math.min(exp, maxMs);
    }

    public long nextDelayMs() {
        long base = baseDelayMs(attempt);
        attempt++;
        long jitter = (long) (jitterRng.getAsDouble() * jitterFraction * base);
        return base + jitter;
    }

    public void reset() {
        attempt = 0;
    }

    public int attempts() {
        return attempt;
    }
}
