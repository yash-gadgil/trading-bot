package io.github.yash_gadgil.tradingbot.testutil;

import io.github.yash_gadgil.tradingbot.app.execution.ExecutionService;
import io.github.yash_gadgil.tradingbot.app.risk.RiskEngine;
import io.github.yash_gadgil.tradingbot.app.risk.RiskProperties;
import io.github.yash_gadgil.tradingbot.core.account.AccountInfoService;
import io.github.yash_gadgil.tradingbot.core.account.AccountSnapshot;
import io.github.yash_gadgil.tradingbot.core.account.HeldPosition;
import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.OrderEvent;
import io.github.yash_gadgil.tradingbot.core.event.OrderFillEvent;
import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;
import io.github.yash_gadgil.tradingbot.core.eventbus.SynchronousEventBus;
import io.github.yash_gadgil.tradingbot.core.position.PositionBook;
import io.github.yash_gadgil.tradingbot.core.strategy.StrategySignalType;

import java.util.ArrayList;
import java.util.List;

public class TradeLoopHarness {

    public final SynchronousEventBus bus = new SynchronousEventBus();
    public final PositionBook book = new PositionBook();
    public final FakeBroker broker = new FakeBroker();
    public final RiskProperties props = new RiskProperties();

    public final List<OrderEvent> orders = new ArrayList<>();
    public final List<OrderFillEvent> fills = new ArrayList<>();

    public double balance = 100_000.0;
    public final List<HeldPosition> openPositions = new ArrayList<>();

    public RiskEngine risk;
    public ExecutionService execution;

    public final AccountInfoService account = new AccountInfoService() {
        @Override
        public AccountSnapshot getAccountSnapshot() {
            return new AccountSnapshot("USD", balance, 1);
        }

        @Override
        public List<HeldPosition> getOpenPositions() {
            return List.copyOf(openPositions);
        }
    };

    public TradeLoopHarness start() {
        bus.subscribe(OrderEvent.class, orders::add);
        bus.subscribe(OrderFillEvent.class, fills::add);
        execution = new ExecutionService(bus, broker);
        risk = new RiskEngine(account, bus, book, props);
        execution.start();
        risk.start();
        return this;
    }

    public void price(String symbol, double close) {
        broker.fillPrices.put(symbol, close);
        bus.publish(new CandleStickEvent(java.time.Instant.now(),
                new io.github.yash_gadgil.tradingbot.core.model.CandleStick(
                        java.time.Instant.now(), symbol, 1_000L, close, close, close, close)));
    }

    public void signal(String symbol, StrategySignalType type) {
        bus.publish(new StrategyEvent("strat", symbol, type));
    }
}
