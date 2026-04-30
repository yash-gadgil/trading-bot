package io.github.yash_gadgil.tradingbot.app.observability;

import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.OrderEvent;
import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;
import io.github.yash_gadgil.tradingbot.core.event.TradeEvent;
import io.github.yash_gadgil.tradingbot.core.eventbus.EventBus;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Profile("metrics")
public class MetricsPublisher implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(MetricsPublisher.class);

    private static final int FLUSH_INTERVAL_MS = 1_000;
    private static final int MAX_BATCH = 500;

    private final EventBus eventBus;
    private final MetricsRepository repo;

    private final ConcurrentLinkedQueue<CandleStick> candleQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MetricsRepository.TickRow> tickQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MetricsRepository.SignalRow> signalQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MetricsRepository.EngineEventRow> engineQueue = new ConcurrentLinkedQueue<>();

    private final ConcurrentHashMap<String, Long> lastBarNanos = new ConcurrentHashMap<>();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    public MetricsPublisher(EventBus eventBus, MetricsRepository repo) {
        this.eventBus = eventBus;
        this.repo = repo;
    }

    @Override
    public void start() {
        eventBus.subscribe(CandleStickEvent.class, this::onCandle);
        eventBus.subscribe(TradeEvent.class, this::onTick);
        eventBus.subscribe(StrategyEvent.class, this::onSignal);
        eventBus.subscribe(OrderEvent.class, this::onOrder);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "metrics-flush");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::flushAll, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);

        running.set(true);
        logger.info("MetricsPublisher started - flushing every {}ms to TimescaleDB", FLUSH_INTERVAL_MS);
    }

    @Override
    public void stop() {
        running.set(false);
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        flushAll();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {

        return 50;
    }

    private void onCandle(CandleStickEvent event) {
        long now = System.nanoTime();
        candleQueue.add(event.candleStick());
        lastBarNanos.put(event.candleStick().symbol(), now);
        engineQueue.add(new MetricsRepository.EngineEventRow(
                Instant.now(), "market-data", "candle", event.candleStick().symbol(), null, null
        ));
    }

    private void onTick(TradeEvent event) {
        tickQueue.add(new MetricsRepository.TickRow(event.timestamp(), event.trade()));
    }

    private void onSignal(StrategyEvent event) {
        Long latencyMs = null;
        Long startNanos = lastBarNanos.get(event.instrument());
        if (startNanos != null) {
            latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
        }
        signalQueue.add(new MetricsRepository.SignalRow(
                Instant.now(), event, null, null, latencyMs
        ));
        if (latencyMs != null) {
            engineQueue.add(new MetricsRepository.EngineEventRow(
                    Instant.now(), "strategy", "signal", event.instrument(), latencyMs, null
            ));
        }
    }

    private void onOrder(OrderEvent event) {
        engineQueue.add(new MetricsRepository.EngineEventRow(
                Instant.now(), "execution",
                event.side() + " " + event.quantity(), event.instrument(), null, null
        ));
    }

    private void flushAll() {
        try {
            flushCandles();
            flushTicks();
            flushSignals();
            flushEngine();
        } catch (Exception e) {

            logger.warn("Metrics flush failed: {}", e.getMessage());
        }
    }

    private void flushCandles() {
        List<CandleStick> batch = drain(candleQueue, candleQueue.size());
        if (batch.isEmpty()) return;
        try {
            repo.insertCandles(batch, "live");
        } catch (Exception e) {
            logger.warn("Dropping {} candle rows - DB write failed: {}", batch.size(), e.getMessage());
        }
    }

    private void flushTicks() {
        List<MetricsRepository.TickRow> batch = drain(tickQueue, MAX_BATCH);
        if (batch.isEmpty()) return;
        try {
            repo.insertTicks(batch);
        } catch (Exception e) {
            logger.warn("Dropping {} tick rows - DB write failed: {}", batch.size(), e.getMessage());
        }
    }

    private void flushSignals() {
        List<MetricsRepository.SignalRow> batch = drain(signalQueue, MAX_BATCH);
        if (batch.isEmpty()) return;
        try {
            repo.insertSignals(batch);
        } catch (Exception e) {
            logger.warn("Dropping {} signal rows - DB write failed: {}", batch.size(), e.getMessage());
        }
    }

    private void flushEngine() {
        List<MetricsRepository.EngineEventRow> batch = drain(engineQueue, MAX_BATCH);
        if (batch.isEmpty()) return;
        try {
            repo.insertEngineEvents(batch);
        } catch (Exception e) {
            logger.warn("Dropping {} engine event rows - DB write failed: {}", batch.size(), e.getMessage());
        }
    }

    private static <T> List<T> drain(ConcurrentLinkedQueue<T> q, int max) {
        List<T> out = new ArrayList<>(Math.min(max, 64));
        for (int i = 0; i < max; i++) {
            T item = q.poll();
            if (item == null) break;
            out.add(item);
        }
        return out;
    }
}
