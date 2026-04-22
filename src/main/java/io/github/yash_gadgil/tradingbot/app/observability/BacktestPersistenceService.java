package io.github.yash_gadgil.tradingbot.app.observability;

import io.github.yash_gadgil.tradingbot.core.backtest.BacktestInsights;
import io.github.yash_gadgil.tradingbot.core.backtest.BacktestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("metrics")
public class BacktestPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(BacktestPersistenceService.class);

    private final MetricsRepository repo;

    public BacktestPersistenceService(MetricsRepository repo) {
        this.repo = repo;
    }

    public String persist(BacktestResult result, BacktestInsights insights) {
        String runId = UUID.randomUUID().toString();
        try {
            repo.insertBacktestRun(runId, result, insights);
            repo.insertSimulatedTrades(runId, result.strategyName(), result.trades());
            repo.insertEquityCurve(runId, result.strategyName(), result.equityCurve());
            logger.info("Persisted backtest run_id={} ({} trades, {} equity points)",
                    runId, result.trades().size(), result.equityCurve().size());
        } catch (Exception e) {
            logger.warn("Failed to persist backtest run_id={}: {}", runId, e.getMessage());
        }
        return runId;
    }
}
