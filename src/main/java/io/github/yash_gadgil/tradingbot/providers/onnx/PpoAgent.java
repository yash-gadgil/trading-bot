package io.github.yash_gadgil.tradingbot.providers.onnx;

import io.github.yash_gadgil.tradingbot.core.model.CandleStick;

import java.util.Collection;

public interface PpoAgent {

    int predict(Collection<CandleStick> history, int logicalPosition);

    Prediction predictWithConfidence(Collection<CandleStick> history, int logicalPosition);

    LatencyStats latencyStats();

    record Prediction(int action, float confidence) {}

    record LatencyStats(long count, double avgMs, double p50Ms, double p99Ms, double minMs, double maxMs) {}
}
