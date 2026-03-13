import json
from pathlib import Path

import numpy as np
import pandas as pd
from stable_baselines3 import PPO

MODEL_STEM = "ppo_trading_agent_multi_new"
WINDOW_BARS = 110

HERE = Path(__file__).resolve().parent
PROJECT_ROOT = HERE.parents[7]
OUT = PROJECT_ROOT / "src" / "test" / "resources" / "golden" / "ppo_observations.json"

def build_observation(history: list[dict], logical_position: int) -> np.ndarray:
    df = pd.DataFrame(history)

    df['return'] = df['close'].pct_change()
    df['sma_10_ratio'] = (df['close'] / df['close'].rolling(window=10).mean()) - 1
    df['sma_30_ratio'] = (df['close'] / df['close'].rolling(window=30).mean()) - 1

    delta = df['close'].diff()
    up = delta.clip(lower=0)
    down = -1 * delta.clip(upper=0)
    ema_up = up.ewm(com=13, adjust=False).mean()
    ema_down = down.ewm(com=13, adjust=False).mean()
    rs = ema_up / (ema_down + 1e-8)
    df['rsi'] = (100 - (100 / (1 + rs))) / 100.0

    ema_12 = df['close'].ewm(span=12, adjust=False).mean()
    ema_26 = df['close'].ewm(span=26, adjust=False).mean()
    macd = ema_12 - ema_26
    signal = macd.ewm(span=9, adjust=False).mean()
    df['macd_hist'] = (macd - signal) / df['close']

    sma_20 = df['close'].rolling(window=20).mean()
    std_20 = df['close'].rolling(window=20).std()
    bb_upper = sma_20 + (2 * std_20)
    bb_lower = sma_20 - (2 * std_20)
    df['bb_pos'] = (df['close'] - bb_lower) / (bb_upper - bb_lower + 1e-8)

    high_low = df['high'] - df['low']
    high_close = np.abs(df['high'] - df['close'].shift())
    low_close = np.abs(df['low'] - df['close'].shift())
    tr = pd.concat([high_low, high_close, low_close], axis=1).max(axis=1)
    df['atr'] = tr.rolling(window=14).mean() / df['close']

    df = df.bfill().fillna(0)

    feature_cols = ['return', 'sma_10_ratio', 'sma_30_ratio', 'rsi', 'macd_hist', 'bb_pos', 'atr', 'volume']
    for col in feature_cols:
        rolling_mean = df[col].rolling(window=100, min_periods=1).mean()
        rolling_std = df[col].rolling(window=100, min_periods=1).std()
        df[col] = (df[col] - rolling_mean) / (rolling_std + 1e-8)

    df = df.bfill().fillna(0)

    features = df[feature_cols].values
    window_feats = features[-10:]
    positions = np.full((10, 1), float(logical_position), dtype=np.float32)
    obs = np.hstack([window_feats, positions]).flatten().astype(np.float32)
    return obs

def make_window(seed: int, symbol: str, regime: str) -> list[dict]:
    rng = np.random.default_rng(seed)
    base = 100.0 + (seed % 7) * 25.0
    if regime == "uptrend":
        drift, vol = 0.0008, 0.004
    elif regime == "downtrend":
        drift, vol = -0.0008, 0.004
    elif regime == "volatile":
        drift, vol = 0.0, 0.015
    else:
        drift, vol = 0.0, 0.002

    closes = [base]
    for _ in range(WINDOW_BARS - 1):
        closes.append(closes[-1] * (1.0 + drift + rng.normal(0, vol)))

    candles = []
    ts = 1_700_000_000
    prev = closes[0]
    for c in closes:
        hi = max(prev, c) * (1.0 + abs(rng.normal(0, vol)))
        lo = min(prev, c) * (1.0 - abs(rng.normal(0, vol)))
        vol_shares = int(1_000_000 + rng.integers(0, 500_000))
        candles.append({
            "timestamp": ts,
            "symbol": symbol,
            "volume": vol_shares,
            "high": round(hi, 4),
            "low": round(lo, 4),
            "open": round(prev, 4),
            "close": round(c, 4),
        })
        prev = c
        ts += 60
    return candles

def main() -> None:
    model = PPO.load(str(HERE / MODEL_STEM), device="cpu")

    regimes = ["uptrend", "downtrend", "volatile", "choppy"]
    symbols = ["AAPL", "MSFT", "TSLA", "BA", "INTC"]
    positions = [0, 1, -1]

    fixtures = []
    seed = 0
    for r_i, regime in enumerate(regimes):
        for s_i in range(4):
            symbol = symbols[(r_i + s_i) % len(symbols)]
            position = positions[(r_i + s_i) % len(positions)]
            candles = make_window(seed, symbol, regime)
            seed += 1
            obs = build_observation(candles, position)
            action, _ = model.predict(obs, deterministic=True)
            fixtures.append({
                "regime": regime,
                "symbol": symbol,
                "position": position,
                "candles": candles,
                "expected_observation": [float(x) for x in obs.tolist()],
                "expected_action": int(action.item()),
            })

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(fixtures, indent=1))
    print(f"Wrote {len(fixtures)} golden fixtures to {OUT}")

if __name__ == "__main__":
    main()
