package io.github.yash_gadgil.tradingbot.core.marketdata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReconnectPolicyTest {

    @Test
    void delaysGrowExponentiallyAndAreCapped() {
        ReconnectPolicy p = new ReconnectPolicy(100, 2000, 2.0, 0.0, () -> 0.0);
        assertEquals(100, p.baseDelayMs(0));
        assertEquals(200, p.baseDelayMs(1));
        assertEquals(400, p.baseDelayMs(2));
        assertEquals(800, p.baseDelayMs(3));
        assertEquals(1600, p.baseDelayMs(4));
        assertEquals(2000, p.baseDelayMs(5));
        assertEquals(2000, p.baseDelayMs(20));
    }

    @Test
    void nextDelayAdvancesAttemptCounter() {
        ReconnectPolicy p = new ReconnectPolicy(100, 2000, 2.0, 0.0, () -> 0.0);
        assertEquals(100, p.nextDelayMs());
        assertEquals(200, p.nextDelayMs());
        assertEquals(400, p.nextDelayMs());
        assertEquals(3, p.attempts());
    }

    @Test
    void jitterStaysWithinBounds() {

        ReconnectPolicy max = new ReconnectPolicy(100, 2000, 2.0, 0.2, () -> 1.0);
        assertEquals(120, max.nextDelayMs());

        ReconnectPolicy mid = new ReconnectPolicy(1000, 5000, 2.0, 0.5, () -> 0.37);
        long base = mid.baseDelayMs(0);
        long d = mid.nextDelayMs();
        assertTrue(d >= base && d <= base + (long) (0.5 * base), "jitter out of bounds: " + d);
    }

    @Test
    void resetClearsAttempts() {
        ReconnectPolicy p = new ReconnectPolicy(100, 2000, 2.0, 0.0, () -> 0.0);
        p.nextDelayMs();
        p.nextDelayMs();
        assertEquals(2, p.attempts());
        p.reset();
        assertEquals(0, p.attempts());
        assertEquals(100, p.nextDelayMs());
    }
}
