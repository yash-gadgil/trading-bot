package io.github.yash_gadgil.tradingbot.app.observability;

import io.github.yash_gadgil.tradingbot.core.backtest.BacktestInsights;
import io.github.yash_gadgil.tradingbot.core.backtest.BacktestResult;
import io.github.yash_gadgil.tradingbot.core.backtest.EquityPoint;
import io.github.yash_gadgil.tradingbot.core.backtest.SimulatedTrade;
import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.model.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@Profile("metrics")
public class MetricsRepository {

    private static final Logger logger = LoggerFactory.getLogger(MetricsRepository.class);

    private final JdbcTemplate jdbc;

    public MetricsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertCandles(List<CandleStick> candles, String source) {
        if (candles.isEmpty()) return;
        jdbc.batchUpdate(
                "INSERT INTO candles (time, symbol, open, high, low, close, volume, source) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                candles,
                candles.size(),
                (ps, c) -> {
                    ps.setTimestamp(1, Timestamp.from(c.timestamp()));
                    ps.setString(2, c.symbol());
                    ps.setDouble(3, c.open());
                    ps.setDouble(4, c.high());
                    ps.setDouble(5, c.low());
                    ps.setDouble(6, c.close());
                    ps.setLong(7, c.volume());
                    ps.setString(8, source);
                }
        );
    }

    public void insertTicks(List<TickRow> ticks) {
        if (ticks.isEmpty()) return;
        jdbc.batchUpdate(
                "INSERT INTO tick_trades (time, symbol, price, size) VALUES (?, ?, ?, ?)",
                ticks,
                ticks.size(),
                (ps, t) -> {
                    ps.setTimestamp(1, Timestamp.from(t.time()));
                    ps.setString(2, t.trade().symbol());
                    ps.setDouble(3, t.trade().price());
                    ps.setInt(4, t.trade().size());
                }
        );
    }

    public void insertSignals(List<SignalRow> signals) {
        if (signals.isEmpty()) return;
        jdbc.batchUpdate(
                "INSERT INTO strategy_signals (time, strategy_id, symbol, signal_type, run_id, price, latency_ms, confidence) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                signals,
                signals.size(),
                (ps, s) -> {
                    ps.setTimestamp(1, Timestamp.from(s.time()));
                    ps.setString(2, s.event().id());
                    ps.setString(3, s.event().instrument());
                    ps.setString(4, s.event().signalType().name());
                    ps.setString(5, s.runId());
                    if (s.price() != null) ps.setDouble(6, s.price()); else ps.setNull(6, java.sql.Types.DOUBLE);
                    if (s.latencyMs() != null) ps.setLong(7, s.latencyMs()); else ps.setNull(7, java.sql.Types.BIGINT);
                    if (s.event().confidence() != null) ps.setDouble(8, s.event().confidence()); else ps.setNull(8, java.sql.Types.DOUBLE);
                }
        );
    }

    public void insertEngineEvents(List<EngineEventRow> events) {
        if (events.isEmpty()) return;
        jdbc.batchUpdate(
                "INSERT INTO engine_events (time, component, event_type, symbol, latency_ms, error_message) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                events,
                events.size(),
                (ps, e) -> {
                    ps.setTimestamp(1, Timestamp.from(e.time()));
                    ps.setString(2, e.component());
                    ps.setString(3, e.eventType());
                    ps.setString(4, e.symbol());
                    if (e.latencyMs() != null) ps.setLong(5, e.latencyMs()); else ps.setNull(5, java.sql.Types.BIGINT);
                    ps.setString(6, e.errorMessage());
                }
        );
    }

    public void insertBacktestRun(String runId, BacktestResult result, BacktestInsights insights) {
        jdbc.update(
                "INSERT INTO backtest_runs (run_id, strategy_id, period_start, period_end, symbols, " +
                        "initial_capital, final_equity, bars_processed, signals_fired, total_trades, " +
                        "win_rate_pct, sharpe_ratio, max_drawdown_pct, profit_factor, total_return_pct) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                runId,
                result.strategyName(),
                Timestamp.from(result.config().startTime().toInstant()),
                Timestamp.from(result.config().endTime().toInstant()),
                result.config().symbols().toArray(new String[0]),
                result.initialCapital(),
                result.finalEquity(),
                result.totalBarsProcessed(),
                result.totalSignals(),
                insights.totalTrades(),
                insights.winRate(),
                insights.sharpeRatio(),
                insights.maxDrawdown(),
                insights.profitFactor(),
                insights.totalReturn()
        );
    }

    public void insertSimulatedTrades(String runId, String strategyId, List<SimulatedTrade> trades) {
        if (trades.isEmpty()) return;
        jdbc.batchUpdate(
                "INSERT INTO simulated_trades (exit_time, entry_time, run_id, strategy_id, symbol, side, " +
                        "entry_price, exit_price, quantity, pnl, return_pct) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                trades,
                trades.size(),
                (ps, t) -> {
                    ps.setTimestamp(1, Timestamp.from(t.exitTime()));
                    ps.setTimestamp(2, Timestamp.from(t.entryTime()));
                    ps.setString(3, runId);
                    ps.setString(4, strategyId);
                    ps.setString(5, t.symbol());
                    ps.setString(6, t.side());
                    ps.setDouble(7, t.entryPrice());
                    ps.setDouble(8, t.exitPrice());
                    ps.setInt(9, t.quantity());
                    ps.setDouble(10, t.pnl());
                    double base = t.entryPrice() * t.quantity();
                    ps.setDouble(11, base == 0 ? 0 : (t.pnl() / base) * 100.0);
                }
        );
    }

    public void insertEquityCurve(String runId, String strategyId, List<EquityPoint> curve) {
        if (curve.isEmpty()) return;
        jdbc.batchUpdate(
                "INSERT INTO equity_snapshots (time, run_id, strategy_id, equity) VALUES (?, ?, ?, ?)",
                curve,
                500,
                (ps, p) -> {
                    ps.setTimestamp(1, Timestamp.from(p.timestamp()));
                    ps.setString(2, runId);
                    ps.setString(3, strategyId);
                    ps.setDouble(4, p.equity());
                }
        );
    }

    public record TickRow(Instant time, Trade trade) {}
    public record SignalRow(Instant time, StrategyEvent event, String runId, Double price, Long latencyMs) {}
    public record EngineEventRow(Instant time, String component, String eventType, String symbol, Long latencyMs, String errorMessage) {}
}
