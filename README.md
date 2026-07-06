# Trading Bot


An event-driven trading bot in Java 21. It streams live market data, runs a PPO
reinforcement-learning agent in-process via ONNX Runtime, places risk checked paper trades on Alpaca, and writes telemetry to
TimescaleDB for Grafana. The same strategy code runs both live and in the backtester.

Made while reading [Building Trading Bots Using Java - Shekhar Varshney](https://link.springer.com/book/10.1007/978-1-4842-2520-2)

## What it does

- Runs the PPO policy inside the JVM through ONNX Runtime. A golden test checks the
  Java feature pipeline against the Python training pipeline to 1e-5.
- Talks over a single in-memory event bus, so one Strategy implementation works live
  and in backtest.
- Sizes orders and runs per-symbol, gross-exposure, and buying-power checks plus a
  daily-loss kill switch before anything reaches the broker. Positions are reconciled
  from the broker on startup.
- Only trades when the PPO agent is above a configurable confidence threshold.
- Backtests with commissions and slippage against a buy-and-hold benchmark, prints the
  metrics to the console, and writes the equity curve and trades to CSV.

## Quick start

Run the full stack with Docker:

```bash
cp .env.example .env         
cd observability
docker compose up             # bot + TimescaleDB + Grafana on http://localhost:3000
```

Or run it locally:

```bash
cp .env.example .env
mvn spring-boot:run           # live paper session
mvn -B verify                 # build + tests + coverage gate
```

Backtest (writes backtest-results/equity.csv and trades.csv):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=backtest \
  -Dspring-boot.run.arguments="--strategy=fade --symbols=AAPL,MSFT --days=30 --commission=0.01 --slippage-bps=10"
```

## Tech Stack

Java 21, Spring Boot 3.4, ONNX Runtime, Alpaca, TimescaleDB, Grafana, JUnit 5,
Mockito, JaCoCo, GitHub Actions.