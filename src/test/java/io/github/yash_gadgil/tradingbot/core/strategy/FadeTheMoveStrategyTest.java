package io.github.yash_gadgil.tradingbot.core.strategy;

import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;
import io.github.yash_gadgil.tradingbot.testutil.CandleFixtures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FadeTheMoveStrategyTest {

    private List<StrategyEvent> feed(double... closes) {
        FadeTheMoveStrategy strategy = new FadeTheMoveStrategy(10, 2.0);
        List<StrategyEvent> signals = new ArrayList<>();
        strategy.setEventPublisher(signals::add);
        for (var c : CandleFixtures.series("AAPL", closes)) {
            strategy.onEvent(new io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent(c.timestamp(), c));
        }
        return signals;
    }

    @Test
    void noSignalUntilWindowIsFull() {

        assertTrue(feed(100, 101, 99, 102, 98, 103, 97, 104, 96).isEmpty());
    }

    @Test
    void entersShortOnHighZScore() {

        List<StrategyEvent> s = feed(100, 100, 100, 100, 100, 100, 100, 100, 100, 130);
        assertEquals(1, s.size());
        assertEquals(StrategySignalType.ENTER_SHORT, s.getFirst().signalType());
    }

    @Test
    void entersLongOnLowZScore() {

        List<StrategyEvent> s = feed(100, 100, 100, 100, 100, 100, 100, 100, 100, 70);
        assertEquals(1, s.size());
        assertEquals(StrategySignalType.ENTER_LONG, s.getFirst().signalType());
    }

    @Test
    void doesNotReEnterWhileAlreadyPositioned_D4() {

        List<StrategyEvent> s = feed(100, 100, 100, 100, 100, 100, 100, 100, 100, 130, 130);
        assertEquals(1, s.size());
        assertEquals(StrategySignalType.ENTER_SHORT, s.getFirst().signalType());
    }

    @Test
    void exitsWhenPriceRevertsThroughMean() {

        List<StrategyEvent> s = feed(100, 100, 100, 100, 100, 100, 100, 100, 100, 130, 70);
        assertEquals(2, s.size());
        assertEquals(StrategySignalType.ENTER_SHORT, s.get(0).signalType());
        assertEquals(StrategySignalType.EXIT, s.get(1).signalType());
    }

    @Test
    void flatWindowEmitsNothing_D3() {

        assertTrue(feed(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100).isEmpty());
    }
}
