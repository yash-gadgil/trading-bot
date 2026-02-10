package io.github.yash_gadgil.tradingbot.testutil;

import io.github.yash_gadgil.tradingbot.core.model.CandleStick;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class CandleFixtures {

    public static final Instant START = Instant.parse("2024-01-02T14:30:00Z");

    private CandleFixtures() {}

    public static CandleStick candle(String symbol, Instant time, double close) {
        return new CandleStick(time, symbol, 1_000L, close, close, close, close);
    }

    public static CandleStick candle(String symbol, double close) {
        return candle(symbol, START, close);
    }

    public static List<CandleStick> series(String symbol, Instant start, double... closes) {
        List<CandleStick> out = new ArrayList<>(closes.length);
        Instant t = start;
        for (double c : closes) {
            out.add(candle(symbol, t, c));
            t = t.plus(Duration.ofMinutes(1));
        }
        return out;
    }

    public static List<CandleStick> series(String symbol, double... closes) {
        return series(symbol, START, closes);
    }
}
