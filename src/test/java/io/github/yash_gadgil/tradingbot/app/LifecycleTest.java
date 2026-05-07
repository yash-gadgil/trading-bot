package io.github.yash_gadgil.tradingbot.app;

import io.github.yash_gadgil.tradingbot.app.execution.ExecutionService;
import io.github.yash_gadgil.tradingbot.app.marketdata.MarketDataStreamingService;
import io.github.yash_gadgil.tradingbot.app.risk.RiskEngine;
import io.github.yash_gadgil.tradingbot.app.risk.RiskProperties;
import io.github.yash_gadgil.tradingbot.app.strategy.StrategyEngine;
import io.github.yash_gadgil.tradingbot.core.account.AccountInfoService;
import io.github.yash_gadgil.tradingbot.core.account.AccountSnapshot;
import io.github.yash_gadgil.tradingbot.core.event.MarketDataEvent;
import io.github.yash_gadgil.tradingbot.core.eventbus.InMemoryEventBus;
import io.github.yash_gadgil.tradingbot.core.eventbus.SynchronousEventBus;
import io.github.yash_gadgil.tradingbot.core.marketdata.MarketDataStreamingProvider;
import io.github.yash_gadgil.tradingbot.core.position.PositionBook;
import io.github.yash_gadgil.tradingbot.testutil.FakeBroker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class LifecycleTest {

    private static final AccountInfoService ACCOUNT = () -> new AccountSnapshot("USD", 100_000, 1);

    @Test
    void executionServiceStartsAndStops() {
        ExecutionService svc = new ExecutionService(new SynchronousEventBus(), new FakeBroker());
        assertFalse(svc.isRunning());
        svc.start();
        assertTrue(svc.isRunning());
        svc.stop();
        assertFalse(svc.isRunning());
    }

    @Test
    void riskEngineStartsAndStops() {
        RiskEngine svc = new RiskEngine(ACCOUNT, new SynchronousEventBus(), new PositionBook(), new RiskProperties());
        assertFalse(svc.isRunning());
        svc.start();
        assertTrue(svc.isRunning());
        svc.stop();
        assertFalse(svc.isRunning());
    }

    @Test
    void marketDataServiceTracksConnectionAndRunningFlag() {
        RecordingProvider provider = new RecordingProvider();
        MarketDataStreamingService svc = new MarketDataStreamingService(provider, new SynchronousEventBus());
        assertFalse(svc.isRunning());
        svc.start();
        assertTrue(svc.isRunning());
        assertTrue(provider.connected);
        svc.stop();
        assertFalse(svc.isRunning());
        assertFalse(provider.connected);
    }

    @Test
    void strategyEngineStartsAndStops() {
        StrategyEngine svc = new StrategyEngine(new SynchronousEventBus(), List.of());
        assertFalse(svc.isRunning());
        svc.start();
        assertTrue(svc.isRunning());
        svc.stop();
        assertFalse(svc.isRunning());
    }

    @Test
    void eventBusShutsDownExecutor() {
        InMemoryEventBus bus = new InMemoryEventBus();
        assertFalse(bus.isShutdown());
        bus.shutdown();
        assertTrue(bus.isShutdown());
    }

    private static final class RecordingProvider implements MarketDataStreamingProvider {
        boolean connected = false;
        @Override public void connect() { connected = true; }
        @Override public void disconnect() { connected = false; }
        @Override public void setEventPublisher(Consumer<MarketDataEvent> publisher) { }
    }
}
