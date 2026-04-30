CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE TABLE IF NOT EXISTS candles (
    time     TIMESTAMPTZ      NOT NULL,
    symbol   TEXT             NOT NULL,
    open     DOUBLE PRECISION NOT NULL,
    high     DOUBLE PRECISION NOT NULL,
    low      DOUBLE PRECISION NOT NULL,
    close    DOUBLE PRECISION NOT NULL,
    volume   BIGINT           NOT NULL,
    source   TEXT             NOT NULL DEFAULT 'live',
    PRIMARY KEY (time, symbol, source)
);

SELECT create_hypertable('candles', 'time', if_not_exists => TRUE, chunk_time_interval => INTERVAL '7 days');
CREATE INDEX IF NOT EXISTS idx_candles_symbol_time ON candles (symbol, time DESC);
CREATE TABLE IF NOT EXISTS tick_trades (
    time   TIMESTAMPTZ      NOT NULL,
    symbol TEXT             NOT NULL,
    price  DOUBLE PRECISION NOT NULL,
    size   INTEGER          NOT NULL
);

SELECT create_hypertable('tick_trades', 'time', if_not_exists => TRUE, chunk_time_interval => INTERVAL '1 day');
CREATE INDEX IF NOT EXISTS idx_tick_trades_symbol_time ON tick_trades (symbol, time DESC);
CREATE TABLE IF NOT EXISTS strategy_signals (
    time         TIMESTAMPTZ NOT NULL,
    strategy_id  TEXT        NOT NULL,
    symbol       TEXT        NOT NULL,
    signal_type  TEXT        NOT NULL,
    run_id       TEXT,
    price        DOUBLE PRECISION,
    latency_ms   BIGINT
);

SELECT create_hypertable('strategy_signals', 'time', if_not_exists => TRUE, chunk_time_interval => INTERVAL '7 days');
CREATE INDEX IF NOT EXISTS idx_signals_strategy_time ON strategy_signals (strategy_id, time DESC);
CREATE INDEX IF NOT EXISTS idx_signals_run ON strategy_signals (run_id) WHERE run_id IS NOT NULL;
CREATE TABLE IF NOT EXISTS simulated_trades (
    exit_time    TIMESTAMPTZ      NOT NULL,
    entry_time   TIMESTAMPTZ      NOT NULL,
    run_id       TEXT             NOT NULL,
    strategy_id  TEXT             NOT NULL,
    symbol       TEXT             NOT NULL,
    side         TEXT             NOT NULL,
    entry_price  DOUBLE PRECISION NOT NULL,
    exit_price   DOUBLE PRECISION NOT NULL,
    quantity     INTEGER          NOT NULL,
    pnl          DOUBLE PRECISION NOT NULL,
    return_pct   DOUBLE PRECISION NOT NULL
);

SELECT create_hypertable('simulated_trades', 'exit_time', if_not_exists => TRUE, chunk_time_interval => INTERVAL '30 days');
CREATE INDEX IF NOT EXISTS idx_trades_run ON simulated_trades (run_id, exit_time DESC);
CREATE INDEX IF NOT EXISTS idx_trades_strategy_symbol ON simulated_trades (strategy_id, symbol, exit_time DESC);
CREATE TABLE IF NOT EXISTS equity_snapshots (
    time         TIMESTAMPTZ      NOT NULL,
    run_id       TEXT             NOT NULL,
    strategy_id  TEXT             NOT NULL,
    equity       DOUBLE PRECISION NOT NULL
);

SELECT create_hypertable('equity_snapshots', 'time', if_not_exists => TRUE, chunk_time_interval => INTERVAL '7 days');
CREATE INDEX IF NOT EXISTS idx_equity_run_time ON equity_snapshots (run_id, time);
CREATE TABLE IF NOT EXISTS backtest_runs (
    run_id            TEXT             PRIMARY KEY,
    strategy_id       TEXT             NOT NULL,
    started_at        TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    period_start      TIMESTAMPTZ      NOT NULL,
    period_end        TIMESTAMPTZ      NOT NULL,
    symbols           TEXT[]           NOT NULL,
    initial_capital   DOUBLE PRECISION NOT NULL,
    final_equity      DOUBLE PRECISION NOT NULL,
    bars_processed    INTEGER          NOT NULL,
    signals_fired     INTEGER          NOT NULL,
    total_trades      INTEGER          NOT NULL,
    win_rate_pct      DOUBLE PRECISION NOT NULL,
    sharpe_ratio      DOUBLE PRECISION NOT NULL,
    max_drawdown_pct  DOUBLE PRECISION NOT NULL,
    profit_factor     DOUBLE PRECISION NOT NULL,
    total_return_pct  DOUBLE PRECISION NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_runs_strategy ON backtest_runs (strategy_id, started_at DESC);
CREATE TABLE IF NOT EXISTS engine_events (
    time          TIMESTAMPTZ NOT NULL,
    component     TEXT        NOT NULL,
    event_type    TEXT        NOT NULL,
    symbol        TEXT,
    latency_ms    BIGINT,
    error_message TEXT
);

SELECT create_hypertable('engine_events', 'time', if_not_exists => TRUE, chunk_time_interval => INTERVAL '1 day');
CREATE INDEX IF NOT EXISTS idx_engine_component_time ON engine_events (component, time DESC);
CREATE INDEX IF NOT EXISTS idx_engine_errors ON engine_events (time DESC) WHERE event_type = 'error';
SELECT add_retention_policy('candles',          INTERVAL '90 days', if_not_exists => TRUE);
SELECT add_retention_policy('tick_trades',      INTERVAL '14 days', if_not_exists => TRUE);
SELECT add_retention_policy('strategy_signals', INTERVAL '180 days', if_not_exists => TRUE);
SELECT add_retention_policy('engine_events',    INTERVAL '30 days', if_not_exists => TRUE);
