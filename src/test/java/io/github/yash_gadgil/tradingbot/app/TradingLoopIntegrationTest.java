package io.github.yash_gadgil.tradingbot.app;

import io.github.yash_gadgil.tradingbot.app.execution.ExecutionService;
import io.github.yash_gadgil.tradingbot.app.risk.RiskEngine;
import io.github.yash_gadgil.tradingbot.app.risk.RiskProperties;
import io.github.yash_gadgil.tradingbot.app.strategy.StrategyEngine;
import io.github.yash_gadgil.tradingbot.core.account.AccountInfoService;
import io.github.yash_gadgil.tradingbot.core.account.AccountSnapshot;
import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.OrderEvent;
import io.github.yash_gadgil.tradingbot.core.eventbus.InMemoryEventBus;
import io.github.yash_gadgil.tradingbot.core.eventbus.SynchronousEventBus;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.order.OrderSide;
import io.github.yash_gadgil.tradingbot.core.position.PositionBook;
import io.github.yash_gadgil.tradingbot.core.strategy.StrategySignalType;
import io.github.yash_gadgil.tradingbot.testutil.CandleFixtures;
import io.github.yash_gadgil.tradingbot.testutil.FakeBroker;
import io.github.yash_gadgil.tradingbot.testutil.ScriptedStrategy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TradingLoopIntegrationTest {

    private static final AccountInfoService ACCOUNT = () -> new AccountSnapshot("USD", 100_000, 1);

    @Test
    void candleProducesSizedOrderAndFillUpdatesBook() {
        var bus = new SynchronousEventBus();
        var book = new PositionBook();
        var broker = new FakeBroker();
        broker.fillPrices.put("AAPL", 100.0);

        var strategy = new ScriptedStrategy("scripted", java.util.Arrays.asList(StrategySignalType.ENTER_LONG, null));
        var strategyEngine = new StrategyEngine(bus, List.of(strategy));
        var riskEngine = new RiskEngine(ACCOUNT, bus, book, new RiskProperties());
        var execution = new ExecutionService(bus, broker);

        strategyEngine.start();
        riskEngine.start();
        execution.start();

        var orders = new java.util.ArrayList<OrderEvent>();
        bus.subscribe(OrderEvent.class, orders::add);

        bus.publish(new CandleStickEvent(CandleFixtures.START, CandleFixtures.candle("AAPL", 100.0)));
        bus.publish(new CandleStickEvent(CandleFixtures.START.plusSeconds(60), CandleFixtures.candle("AAPL", 101.0)));

        assertEquals(1, orders.size(), "exactly one entry order");
        OrderEvent order = orders.getFirst();
        assertEquals(OrderSide.BUY, order.side());
        assertEquals("AAPL", order.instrument());
        assertEquals(10, order.quantity());
        assertEquals(1, broker.placed.size());
        assertEquals(10, book.quantityOf("AAPL"));
        assertEquals(100.0, book.get("AAPL").orElseThrow().avgEntryPrice());
    }

    @Test
    void realAsyncBusDeliversTheLoopEndToEnd() throws Exception {
        var bus = new InMemoryEventBus();
        var book = new PositionBook();
        var broker = new FakeBroker();
        broker.fillPrices.put("AAPL", 100.0);

        var strategy = new ScriptedStrategy("scripted", List.of(StrategySignalType.ENTER_LONG));
        new StrategyEngine(bus, List.of(strategy)).start();
        new RiskEngine(ACCOUNT, bus, book, new RiskProperties()).start();
        new ExecutionService(bus, broker).start();

        bus.publish(new CandleStickEvent(Instant.now(),
                new CandleStick(Instant.now(), "AAPL", 1_000L, 100.0, 100.0, 100.0, 100.0)));

        long deadline = System.currentTimeMillis() + 2_000;
        while (book.quantityOf("AAPL") == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(10, book.quantityOf("AAPL"), "fill should propagate through the async bus");
        bus.shutdown();
    }
}
