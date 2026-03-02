from flask import Flask, request, jsonify
from stable_baselines3 import PPO
import pandas as pd
import numpy as np

app = Flask(__name__)

model = PPO.load("ppo_trading_agent_multi_new")

@app.route("/predict", methods=["POST"])
def predict():
    data = request.json
    logical_position = data['logical_position']
    df = pd.DataFrame(data['history'])

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

    window_size = 10
    window_feats = features[-window_size:]

    positions = np.full((window_size, 1), float(logical_position), dtype=np.float32)
    obs = np.hstack([window_feats, positions]).flatten().astype(np.float32)

    action, _ = model.predict(obs, deterministic=True)

    return jsonify({"action": int(action.item())})

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000)
