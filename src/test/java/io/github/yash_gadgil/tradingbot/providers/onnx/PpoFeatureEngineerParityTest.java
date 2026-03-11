package io.github.yash_gadgil.tradingbot.providers.onnx;

import io.github.yash_gadgil.tradingbot.testutil.GoldenFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PpoFeatureEngineerParityTest {

    @Test
    void javaObservationsMatchPythonWithin1e5() throws Exception {
        List<GoldenFixtures.Fixture> fixtures = GoldenFixtures.load();
        assertFalse(fixtures.isEmpty(), "expected golden fixtures to be present");

        for (GoldenFixtures.Fixture f : fixtures) {
            float[] actual = PpoFeatureEngineer.buildObservation(f.candles(), f.position());
            assertEquals(PpoFeatureEngineer.OBSERVATION_SIZE, actual.length);
            for (int i = 0; i < actual.length; i++) {
                assertEquals(f.expectedObservation()[i], actual[i], 1e-5,
                        "regime=" + f.regime() + " symbol=" + f.symbol() + " obs[" + i + "]");
            }
        }
    }
}
