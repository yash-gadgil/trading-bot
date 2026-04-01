package io.github.yash_gadgil.tradingbot.app.backtest;

import io.github.yash_gadgil.tradingbot.app.observability.BacktestPersistenceService;
import io.github.yash_gadgil.tradingbot.core.backtest.*;
import io.github.yash_gadgil.tradingbot.core.marketdata.HistoricMarketDataProvider;
import io.github.yash_gadgil.tradingbot.core.strategy.FadeTheMoveStrategy;
import io.github.yash_gadgil.tradingbot.core.strategy.PpoStrategy;
import io.github.yash_gadgil.tradingbot.core.strategy.Strategy;
import io.github.yash_gadgil.tradingbot.providers.onnx.PpoAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

@Component
@Profile("backtest")
public class BacktestRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(BacktestRunner.class);

    private static final Set<String> PPO_DEFAULT_SYMBOLS = Set.of("AAPL", "TSLA", "MSFT", "BA", "INTC");
    private static final Set<String> FADE_DEFAULT_SYMBOLS = Set.of("AAPL", "GOOGL", "MSFT");

    private final HistoricMarketDataProvider marketDataProvider;
    private final Optional<BacktestPersistenceService> persistence;

    public BacktestRunner(HistoricMarketDataProvider marketDataProvider,
                          Optional<BacktestPersistenceService> persistence) {
        this.marketDataProvider = marketDataProvider;
        this.persistence = persistence;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Backtest simulation mode");

        BacktestArgs a = parseArgs(args);

        OffsetDateTime endTime = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startTime = endTime.minusDays(a.days());

        Strategy strategy = buildStrategy(a.strategyName());

        logger.info("Strategy   : {}", strategy.id());
        logger.info("Symbols    : {}", a.symbols());
        logger.info("Period     : {} -> {} ({} days)", startTime.toLocalDate(), endTime.toLocalDate(), a.days());
        logger.info("Capital    : ${}", String.format("%,.2f", a.capital()));
        logger.info("Pos size   : {} shares", a.positionSize());
        logger.info("Costs      : ${}/share commission, {} bps slippage", a.commission(), a.slippageBps());

        BacktestConfig config = new BacktestConfig(
                a.symbols(), startTime, endTime, a.capital(), a.positionSize(), a.commission(), a.slippageBps());
        BacktestEngine engine = new BacktestEngine(strategy, marketDataProvider, config);

        BacktestResult result;
        try {
            result = engine.run();
        } catch (io.github.yash_gadgil.tradingbot.core.marketdata.MarketDataException e) {
            logger.error("Backtest aborted - could not load market data: {}", e.getMessage(), e);
            throw e;
        }

        BacktestInsights insights = BacktestInsights.from(result);
        logInsights(insights);

        if (strategy instanceof PpoStrategy ppo) {
            PpoAgent.LatencyStats stats = ppo.agent().latencyStats();
            logger.info("PPO inference latency");
            logger.info("  Inferences:      {}", stats.count());
            logger.info("  Avg latency:     {} ms", String.format("%.3f", stats.avgMs()));
            logger.info("  p50 latency:     {} ms", String.format("%.3f", stats.p50Ms()));
            logger.info("  p99 latency:     {} ms", String.format("%.3f", stats.p99Ms()));
            logger.info("  Min latency:     {} ms", String.format("%.3f", stats.minMs()));
            logger.info("  Max latency:     {} ms", String.format("%.3f", stats.maxMs()));
        }

        persistence.ifPresent(p -> {
            String runId = p.persist(result, insights);
            logger.info("Backtest run persisted: run_id={}", runId);
        });

        Path outDir = Path.of("backtest-results").toAbsolutePath();
        BacktestCsvWriter.write(result, outDir);
        logger.info("Results written to: {}", outDir);
    }

    record BacktestArgs(String strategyName, Set<String> symbols, int days,
                        double capital, int positionSize, double commission, double slippageBps) {}

    static BacktestArgs parseArgs(String[] args) {
        String strategyName = "ppo";
        Set<String> symbols = null;
        Integer days = null;
        double capital = 100_000.0;
        int positionSize = 10;
        double commission = 0.0;
        double slippageBps = 0.0;

        for (String arg : args) {
            if (arg.startsWith("--strategy=")) {
                strategyName = arg.substring("--strategy=".length()).toLowerCase().strip();
            } else if (arg.startsWith("--symbols=")) {
                symbols = Set.of(arg.substring("--symbols=".length()).split(","));
            } else if (arg.startsWith("--days=")) {
                days = Integer.parseInt(arg.substring("--days=".length()));
            } else if (arg.startsWith("--capital=")) {
                capital = Double.parseDouble(arg.substring("--capital=".length()));
            } else if (arg.startsWith("--position-size=")) {
                positionSize = Integer.parseInt(arg.substring("--position-size=".length()));
            } else if (arg.startsWith("--commission=")) {
                commission = Double.parseDouble(arg.substring("--commission=".length()));
            } else if (arg.startsWith("--slippage-bps=")) {
                slippageBps = Double.parseDouble(arg.substring("--slippage-bps=".length()));
            }
        }

        if ("ppo".equals(strategyName)) {
            if (symbols == null) symbols = PPO_DEFAULT_SYMBOLS;
            if (days == null) days = 90;
        } else {
            if (symbols == null) symbols = FADE_DEFAULT_SYMBOLS;
            if (days == null) days = 30;
        }
        return new BacktestArgs(strategyName, symbols, days, capital, positionSize, commission, slippageBps);
    }

    Strategy buildStrategy(String name) {
        return switch (name) {
            case "ppo"  -> {
                logger.info("Loading PPO ONNX model...");
                yield new PpoStrategy();
            }
            case "fade" -> new FadeTheMoveStrategy(14, 2.0);
            default     -> throw new IllegalArgumentException(
                    "Unknown --strategy value '" + name + "'. Valid options: ppo, fade");
        };
    }

    private void logInsights(BacktestInsights insights) {
        logger.info("Backtest insights");
        logger.info("  Total Return:    {}%", String.format("%+.2f", insights.totalReturn()));
        logger.info("  Benchmark Ret:   {}%", String.format("%+.2f", insights.benchmarkReturn()));
        logger.info("  Alpha:           {}%", String.format("%+.2f", insights.alpha()));
        logger.info("  Total P&L:       ${}", String.format("%+,.2f", insights.totalPnl()));
        logger.info("  Total Costs:     ${}", String.format("%,.2f", insights.totalCosts()));
        logger.info("  Sharpe Ratio:    {}", String.format("%.2f", insights.sharpeRatio()));
        logger.info("  Sortino Ratio:   {}", String.format("%.2f", insights.sortinoRatio()));
        logger.info("  Max Drawdown:    {}%", String.format("%.2f", insights.maxDrawdown()));
        logger.info("  Win Rate:        {}%", String.format("%.1f", insights.winRate()));
        logger.info("  Total Trades:    {}", insights.totalTrades());
        logger.info("  Profit Factor:   {}", String.format("%.2f", insights.profitFactor()));
        logger.info("  Avg Win:         ${}", String.format("%+,.2f", insights.averageWin()));
        logger.info("  Avg Loss:        -${}", String.format("%,.2f", insights.averageLoss()));
        logger.info("  Largest Win:     ${}", String.format("%+,.2f", insights.largestWin()));
        logger.info("  Largest Loss:    -${}", String.format("%,.2f", Math.abs(insights.largestLoss())));
    }
}
