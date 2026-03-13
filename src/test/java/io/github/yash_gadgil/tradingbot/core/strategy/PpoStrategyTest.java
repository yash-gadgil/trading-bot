package io.github.yash_gadgil.tradingbot.core.strategy;

import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.providers.onnx.PpoAgent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PpoStrategyTest {

    private static final class StubAgent implements PpoAgent {
        Prediction next = new Prediction(3, 1.0f);
        boolean throwOnCall = false;

        @Override public int predict(Collection<CandleStick> h, int p) { return next.action(); }
        @Override public Prediction predictWithConfidence(Collection<CandleStick> h, int p) {
            if (throwOnCall) throw new RuntimeException("inference boom");
            return next;
        }
        @Override public LatencyStats latencyStats() { return new LatencyStats(0, 0, 0, 0, 0, 0); }
    }

    private static List<StrategyEvent> feed(PpoStrategy strategy, int bars) {
        List<StrategyEvent> signals = new ArrayList<>();
        strategy.setEventPublisher(signals::add);
        Instant t = Instant.parse("2024-01-02T14:30:00Z");
        for (int i = 0; i < bars; i++) {
            CandleStick c = new CandleStick(t.plusSeconds(i * 60L), "AAPL", 1_000L, 100.0, 100.0, 100.0, 100.0 + i);
            strategy.onEvent(new CandleStickEvent(c.timestamp(), c));
        }
        return signals;
    }

    @Test
    void emitsNothingUntilWindowIsFull() {
        StubAgent agent = new StubAgent();
        agent.next = new PpoAgent.Prediction(0, 0.9f);
        assertTrue(feed(new PpoStrategy(agent, 0.0), 109).isEmpty());
        assertEquals(1, feed(new PpoStrategy(agent, 0.0), 110).size());
    }

    @Test
    void mapsActionsToSignals() {
        assertSignal(0, StrategySignalType.ENTER_LONG);
        assertSignal(1, StrategySignalType.ENTER_SHORT);
        assertSignal(2, StrategySignalType.EXIT);
        assertSignal(4, StrategySignalType.REDUCE);
    }

    private void assertSignal(int action, StrategySignalType expected) {
        StubAgent agent = new StubAgent();
        agent.next = new PpoAgent.Prediction(action, 0.9f);
        List<StrategyEvent> s = feed(new PpoStrategy(agent, 0.0), 110);
        assertEquals(1, s.size());
        assertEquals(expected, s.getFirst().signalType());
    }

    @Test
    void holdActionEmitsNoEvent() {
        StubAgent agent = new StubAgent();
        agent.next = new PpoAgent.Prediction(3, 0.9f);
        assertTrue(feed(new PpoStrategy(agent, 0.0), 110).isEmpty());
    }

    @Test
    void belowConfidenceThresholdIsGated() {
        StubAgent agent = new StubAgent();
        agent.next = new PpoAgent.Prediction(0, 0.40f);
        assertTrue(feed(new PpoStrategy(agent, 0.55), 110).isEmpty());
    }

    @Test
    void aboveThresholdEmitsWithConfidenceAttached() {
        StubAgent agent = new StubAgent();
        agent.next = new PpoAgent.Prediction(0, 0.80f);
        List<StrategyEvent> s = feed(new PpoStrategy(agent, 0.55), 110);
        assertEquals(1, s.size());
        assertEquals(0.80, s.getFirst().confidence(), 1e-6);
    }

    @Test
    void inferenceFailureIsSwallowed() {
        StubAgent agent = new StubAgent();
        agent.throwOnCall = true;
        assertDoesNotThrow(() -> assertTrue(feed(new PpoStrategy(agent, 0.0), 110).isEmpty()));
    }
}
