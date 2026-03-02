# PPO trading agent

The trading bot runs the PPO policy **in-process in Java** via ONNX Runtime. No
Flask sidecar is needed at runtime; the Python code here is used only for
training and exporting the model.

## Files

- `ppo.ipynb` - training notebook (stable-baselines3 + gymnasium `TradingEnv`).
- `ppo_trading_agent_multi_new.zip` - latest SB3-format checkpoint.
- `export_onnx.py` - converts the SB3 policy into an ONNX graph that the Java
  runtime can load. Writes to
  `src/main/resources/models/ppo_trading_agent_multi_new.onnx`.
- `ppo_server.py` - **legacy** Flask server. Kept for reference / debugging;
  the production path no longer uses it.

## Updating the deployed model

1. Retrain (or load) the PPO checkpoint in `ppo.ipynb` and save it as
   `ppo_trading_agent_multi_new.zip` in this directory.
2. From this directory, run:
   ```
   uv run python export_onnx.py
   ```
   This produces `src/main/resources/models/ppo_trading_agent_multi_new.onnx`.
3. Rebuild the Spring Boot app (`./mvnw package`). `OnnxPpoAgent` will pick up
   the new file from the classpath at startup.

## Runtime flow

```
Market data ► PpoStrategy.onEvent

                     ▼
              110-bar candle cache

                     ▼
         PpoFeatureEngineer (Java port of
         the pandas indicator pipeline)

                     ▼
             float[90] observation

                     ▼
          OnnxPpoAgent.predict ► ONNX Runtime

                     ▼
               argmax(logits) ∈ {0..4}

                     ▼
        StrategySignalType (LONG/SHORT/EXIT/HOLD/REDUCE)
```

The observation layout matches the training environment exactly: a 10x9 matrix
(last 10 bars x {return, sma_10_ratio, sma_30_ratio, rsi, macd_hist, bb_pos,
atr, volume, logical_position}) flattened row-major into 90 floats. Each of the
eight indicator columns is rolling z-score normalised over a 100-bar window
before the slice is taken, identical to the training pipeline.
