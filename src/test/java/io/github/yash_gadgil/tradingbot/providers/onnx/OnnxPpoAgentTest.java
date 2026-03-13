package io.github.yash_gadgil.tradingbot.providers.onnx;

import io.github.yash_gadgil.tradingbot.testutil.GoldenFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OnnxPpoAgentTest {

    private OnnxPpoAgent agent;
    private List<GoldenFixtures.Fixture> fixtures;

    @BeforeAll
    void setUp() throws Exception {
        agent = new OnnxPpoAgent();
        fixtures = GoldenFixtures.load();
    }

    @AfterAll
    void tearDown() throws Exception {
        agent.close();
    }

    @Test
    void matchesPythonActionsForEveryFixture() {
        for (GoldenFixtures.Fixture f : fixtures) {
            assertEquals(f.expectedAction(), agent.predict(f.expectedObservation()),
                    "regime=" + f.regime() + " symbol=" + f.symbol());
        }
    }

    @Test
    void predictionIsDeterministic() {
        float[] obs = fixtures.getFirst().expectedObservation();
        assertEquals(agent.predict(obs), agent.predict(obs));
    }

    @Test
    void confidenceIsAValidProbability() {
        for (GoldenFixtures.Fixture f : fixtures) {
            PpoAgent.Prediction p = agent.predictWithConfidence(f.expectedObservation());
            assertEquals(f.expectedAction(), p.action());

            assertTrue(p.confidence() > 0.199f && p.confidence() <= 1.0001f,
                    "confidence out of range: " + p.confidence());
        }
    }

    @Test
    void wrongObservationLengthThrows() {
        assertThrows(IllegalArgumentException.class, () -> agent.predict(new float[10]));
    }

    @Test
    void latencyStatsCountsCalls() throws Exception {
        try (OnnxPpoAgent fresh = new OnnxPpoAgent()) {
            float[] obs = fixtures.getFirst().expectedObservation();
            for (int i = 0; i < 5; i++) fresh.predict(obs);
            assertEquals(5, fresh.latencyStats().count());
        }
    }
}
