package io.github.yash_gadgil.tradingbot.providers.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import io.github.yash_gadgil.tradingbot.core.model.CandleStick;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Map;

public final class OnnxPpoAgent implements PpoAgent, AutoCloseable {

    public static final String DEFAULT_RESOURCE_PATH = "models/ppo_trading_agent_multi_new.onnx";

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final long[] inputShape = {1L, PpoFeatureEngineer.OBSERVATION_SIZE};

    private static final int LATENCY_SAMPLE_CAP = 200_000;
    private final long[] latencySamplesNs = new long[LATENCY_SAMPLE_CAP];
    private int latencySampleCount = 0;
    private long latencyTotalCount = 0;
    private long latencySumNs = 0;
    private long latencyMinNs = Long.MAX_VALUE;
    private long latencyMaxNs = 0;

    public OnnxPpoAgent() {
        this(DEFAULT_RESOURCE_PATH);
    }

    public OnnxPpoAgent(String classpathResource) {
        byte[] modelBytes = loadResource(classpathResource);
        try {
            this.environment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            this.session = environment.createSession(modelBytes, options);
            this.inputName = session.getInputNames().iterator().next();
        } catch (OrtException e) {
            throw new IllegalStateException("Failed to initialise ONNX session for PPO policy", e);
        }
    }

    @Override
    public int predict(Collection<CandleStick> history, int logicalPosition) {
        return predict(PpoFeatureEngineer.buildObservation(history, logicalPosition));
    }

    @Override
    public Prediction predictWithConfidence(Collection<CandleStick> history, int logicalPosition) {
        return predictWithConfidence(PpoFeatureEngineer.buildObservation(history, logicalPosition));
    }

    public int predict(float[] observation) {
        return argmax(logits(observation));
    }

    public Prediction predictWithConfidence(float[] observation) {
        float[] logits = logits(observation);
        int action = argmax(logits);
        float confidence = softmax(logits)[action];
        return new Prediction(action, confidence);
    }

    private float[] logits(float[] observation) {
        if (observation.length != PpoFeatureEngineer.OBSERVATION_SIZE) {
            throw new IllegalArgumentException(
                    "Expected observation of length " + PpoFeatureEngineer.OBSERVATION_SIZE
                            + ", got " + observation.length);
        }
        long start = System.nanoTime();
        try (OnnxTensor input = OnnxTensor.createTensor(
                environment, FloatBuffer.wrap(observation), inputShape);
             OrtSession.Result output = session.run(Map.of(inputName, input))) {

            float[][] logits = (float[][]) output.get(0).getValue();
            recordLatency(System.nanoTime() - start);
            return logits[0];
        } catch (OrtException e) {
            throw new IllegalStateException("ONNX inference failed", e);
        }
    }

    private static float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float l : logits) max = Math.max(max, l);
        float sum = 0;
        float[] out = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            out[i] = (float) Math.exp(logits[i] - max);
            sum += out[i];
        }
        for (int i = 0; i < out.length; i++) out[i] /= sum;
        return out;
    }

    private void recordLatency(long ns) {
        latencyTotalCount++;
        latencySumNs += ns;
        if (ns < latencyMinNs) latencyMinNs = ns;
        if (ns > latencyMaxNs) latencyMaxNs = ns;
        if (latencySampleCount < LATENCY_SAMPLE_CAP) {
            latencySamplesNs[latencySampleCount++] = ns;
        }
    }

    @Override
    public LatencyStats latencyStats() {
        if (latencyTotalCount == 0) {
            return new LatencyStats(0, 0, 0, 0, 0, 0);
        }
        long[] sorted = new long[latencySampleCount];
        System.arraycopy(latencySamplesNs, 0, sorted, 0, latencySampleCount);
        java.util.Arrays.sort(sorted);
        double p50 = sorted[(int) Math.min(sorted.length - 1, Math.round(sorted.length * 0.50))] / 1_000_000.0;
        double p99 = sorted[(int) Math.min(sorted.length - 1, Math.round(sorted.length * 0.99))] / 1_000_000.0;
        double avg = (latencySumNs / (double) latencyTotalCount) / 1_000_000.0;
        return new LatencyStats(
                latencyTotalCount,
                avg,
                p50,
                p99,
                latencyMinNs / 1_000_000.0,
                latencyMaxNs / 1_000_000.0
        );
    }

    @Override
    public void close() throws OrtException {
        session.close();
    }

    private static int argmax(float[] logits) {
        int best = 0;
        float bestValue = logits[0];
        for (int i = 1; i < logits.length; i++) {
            if (logits[i] > bestValue) {
                bestValue = logits[i];
                best = i;
            }
        }
        return best;
    }

    private static byte[] loadResource(String resource) {
        ClassLoader cl = OnnxPpoAgent.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(
                        "ONNX model not found on classpath at '" + resource + "'. "
                                + "Run src/main/java/io/github/yash_gadgil/tradingbot/model/export_onnx.py first.");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read ONNX model resource '" + resource + "'", e);
        }
    }

}
