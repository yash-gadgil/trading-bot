package io.github.yash_gadgil.tradingbot.testutil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class GoldenFixtures {

    public record Fixture(String regime, String symbol, int position,
                          List<CandleStick> candles, float[] expectedObservation, int expectedAction) {}

    private GoldenFixtures() {}

    public static List<Fixture> load() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = GoldenFixtures.class.getResourceAsStream("/golden/ppo_observations.json")) {
            if (in == null) {
                throw new IllegalStateException("Golden fixtures not found on test classpath at /golden/ppo_observations.json");
            }
            JsonNode root = mapper.readTree(in);
            List<Fixture> out = new ArrayList<>();
            for (JsonNode f : root) {
                List<CandleStick> candles = new ArrayList<>();
                for (JsonNode c : f.get("candles")) {
                    candles.add(new CandleStick(
                            Instant.ofEpochSecond(c.get("timestamp").asLong()),
                            c.get("symbol").asText(),
                            c.get("volume").asLong(),
                            c.get("high").asDouble(),
                            c.get("low").asDouble(),
                            c.get("open").asDouble(),
                            c.get("close").asDouble()));
                }
                JsonNode obsNode = f.get("expected_observation");
                float[] obs = new float[obsNode.size()];
                for (int i = 0; i < obs.length; i++) obs[i] = (float) obsNode.get(i).asDouble();
                out.add(new Fixture(
                        f.get("regime").asText(), f.get("symbol").asText(), f.get("position").asInt(),
                        candles, obs, f.get("expected_action").asInt()));
            }
            return out;
        }
    }
}
