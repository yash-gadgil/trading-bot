package io.github.yash_gadgil.tradingbot.core.backtest;

import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;
import io.github.yash_gadgil.tradingbot.core.marketdata.HistoricMarketDataProvider;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.strategy.Strategy;
import io.github.yash_gadgil.tradingbot.core.strategy.StrategySignalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

public class BacktestEngine {

    private static final Logger logger = LoggerFactory.getLogger(BacktestEngine.class);

    private final Strategy strategy;
    private final HistoricMarketDataProvider marketDataProvider;
    private final BacktestConfig config;
    private final double slip;

    private final Map<String, OpenPosition> openPositions = new HashMap<>();
    private final List<SimulatedTrade> completedTrades = new ArrayList<>();
    private final List<EquityPoint> equityCurve = new ArrayList<>();
    private final List<EquityPoint> benchmarkCurve = new ArrayList<>();
    private final List<StrategyEvent> capturedSignals = new ArrayList<>();

    private final Map<String, Double> lastPrice = new HashMap<>();

    private final Map<String, Double> benchmarkShares = new HashMap<>();
    private double benchmarkCash;

    private double cash;
    private double unrealizedPnl;
    private double totalCosts;

    public BacktestEngine(Strategy strategy, HistoricMarketDataProvider marketDataProvider, BacktestConfig config) {
        this.strategy = strategy;
        this.marketDataProvider = marketDataProvider;
        this.config = config;
        this.cash = config.initialCapital();
        this.benchmarkCash = config.initialCapital();
        this.slip = config.slippageBps() / 10_000.0;
    }

    public BacktestResult run() {
        logger.info("Backtest starting");
        logger.info("  Strategy: {}", strategy.id());
        logger.info("  Symbols:  {}", config.symbols());
        logger.info("  Period:   {} -> {}", config.startTime(), config.endTime());
        logger.info("  Capital:  ${}", String.format("%,.2f", config.initialCapital()));
        logger.info("  Costs:    ${}/share, {} bps slippage",
                config.commissionPerShare(), config.slippageBps());

        List<CandleStick> candles = marketDataProvider.getHistoricalCandleSticks(
                config.symbols(), config.startTime(), config.endTime());

        if (candles.isEmpty()) {
            logger.warn("No historical data returned. Check symbols and date range.");
            return emptyResult();
        }

        logger.info("Fetched {} candles", candles.size());

        candles = candles.stream()
                .sorted(Comparator.comparing(CandleStick::timestamp))
                .toList();

        strategy.setEventPublisher(capturedSignals::add);

        int barCount = 0;
        int signalsBefore;

        for (CandleStick candle : candles) {
            barCount++;

            signalsBefore = capturedSignals.size();
            strategy.onEvent(new CandleStickEvent(candle.timestamp(), candle));

            while (capturedSignals.size() > signalsBefore) {
                processSignal(capturedSignals.get(signalsBefore), candle);
                signalsBefore++;
            }

            lastPrice.put(candle.symbol(), candle.close());
            updateBenchmark(candle);

            updateUnrealizedPnl();
            equityCurve.add(new EquityPoint(candle.timestamp(), cash + unrealizedPnl));
            benchmarkCurve.add(new EquityPoint(candle.timestamp(), benchmarkValue()));

            if (barCount % 1000 == 0) {
                logger.info("  Processed {} bars, {} trades completed", barCount, completedTrades.size());
            }
        }

        forceCloseAllPositions(candles.getLast().timestamp());

        double finalEquity = cash;

        logger.info("Backtest complete");
        logger.info("  Bars processed: {}", barCount);
        logger.info("  Signals fired:  {}", capturedSignals.size());
        logger.info("  Trades closed:  {}", completedTrades.size());
        logger.info("  Total costs:    ${}", String.format("%,.2f", totalCosts));
        logger.info("  Final equity:   ${}", String.format("%,.2f", finalEquity));

        return new BacktestResult(
                strategy.id(), config,
                List.copyOf(completedTrades), List.copyOf(equityCurve), List.copyOf(benchmarkCurve),
                config.initialCapital(), finalEquity, barCount, capturedSignals.size(), totalCosts);
    }

    private BacktestResult emptyResult() {
        return new BacktestResult(strategy.id(), config, List.of(), List.of(), List.of(),
                config.initialCapital(), config.initialCapital(), 0, 0, 0.0);
    }

    private void processSignal(StrategyEvent signal, CandleStick currentCandle) {
        String symbol = signal.instrument();
        StrategySignalType type = signal.signalType();
        double price = currentCandle.close();

        switch (type) {
            case ENTER_LONG -> {
                if (isShort(symbol)) closePosition(symbol, price, currentCandle.timestamp());
                if (!openPositions.containsKey(symbol)) openPosition(symbol, "LONG", price, currentCandle.timestamp());
            }
            case ENTER_SHORT -> {
                if (isLong(symbol)) closePosition(symbol, price, currentCandle.timestamp());
                if (!openPositions.containsKey(symbol)) openPosition(symbol, "SHORT", price, currentCandle.timestamp());
            }
            case EXIT -> {
                if (openPositions.containsKey(symbol)) closePosition(symbol, price, currentCandle.timestamp());
            }
            case HOLD, REDUCE -> {  }
        }
    }

    private boolean isLong(String symbol) {
        OpenPosition p = openPositions.get(symbol);
        return p != null && p.side.equals("LONG");
    }

    private boolean isShort(String symbol) {
        OpenPosition p = openPositions.get(symbol);
        return p != null && p.side.equals("SHORT");
    }

    private void openPosition(String symbol, String side, double marketPrice, Instant time) {
        int qty = config.positionSizeShares();
        double fill = side.equals("LONG") ? marketPrice * (1 + slip) : marketPrice * (1 - slip);
        double commission = config.commissionPerShare() * qty;
        double cost = fill * qty + commission;

        if (cost > cash) {
            logger.debug("Insufficient cash for {} {} {} @ ${}", side, qty, symbol, String.format("%.2f", fill));
            return;
        }

        cash -= cost;
        totalCosts += commission + Math.abs(fill - marketPrice) * qty;
        openPositions.put(symbol, new OpenPosition(symbol, side, fill, qty, time));
    }

    private void closePosition(String symbol, double marketPrice, Instant exitTime) {
        OpenPosition pos = openPositions.remove(symbol);
        if (pos == null) return;

        double fill = pos.side.equals("LONG") ? marketPrice * (1 - slip) : marketPrice * (1 + slip);
        double commission = config.commissionPerShare() * pos.quantity;
        double pnl = pos.side.equals("LONG")
                ? (fill - pos.entryPrice) * pos.quantity
                : (pos.entryPrice - fill) * pos.quantity;

        cash += (pos.entryPrice * pos.quantity) + pnl - commission;
        totalCosts += commission + Math.abs(fill - marketPrice) * pos.quantity;

        completedTrades.add(new SimulatedTrade(
                pos.entryTime, exitTime, symbol, pos.side, pos.entryPrice, fill, pos.quantity, pnl));
    }

    private void updateUnrealizedPnl() {
        unrealizedPnl = 0;
        for (OpenPosition pos : openPositions.values()) {
            double price = lastPrice.getOrDefault(pos.symbol, pos.entryPrice);
            unrealizedPnl += pos.side.equals("LONG")
                    ? (price - pos.entryPrice) * pos.quantity
                    : (pos.entryPrice - price) * pos.quantity;
        }
    }

    private void forceCloseAllPositions(Instant exitTime) {
        for (String symbol : new ArrayList<>(openPositions.keySet())) {
            closePosition(symbol, lastPrice.getOrDefault(symbol, openPositions.get(symbol).entryPrice), exitTime);
        }
    }

    private void updateBenchmark(CandleStick candle) {

        if (!benchmarkShares.containsKey(candle.symbol())) {
            double allocation = config.initialCapital() / config.symbols().size();
            double shares = allocation / candle.close();
            benchmarkShares.put(candle.symbol(), shares);
            benchmarkCash -= allocation;
        }
    }

    private double benchmarkValue() {
        double value = benchmarkCash;
        for (Map.Entry<String, Double> e : benchmarkShares.entrySet()) {
            value += e.getValue() * lastPrice.getOrDefault(e.getKey(), 0.0);
        }
        return value;
    }

    private static final class OpenPosition {
        final String symbol;
        final String side;
        final double entryPrice;
        final int quantity;
        final Instant entryTime;

        OpenPosition(String symbol, String side, double entryPrice, int quantity, Instant entryTime) {
            this.symbol = symbol;
            this.side = side;
            this.entryPrice = entryPrice;
            this.quantity = quantity;
            this.entryTime = entryTime;
        }
    }
}
