package io.github.yash_gadgil.tradingbot.app.risk;

import io.github.yash_gadgil.tradingbot.core.account.AccountInfoService;
import io.github.yash_gadgil.tradingbot.core.account.AccountSnapshot;
import io.github.yash_gadgil.tradingbot.core.account.HeldPosition;
import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.OrderEvent;
import io.github.yash_gadgil.tradingbot.core.event.OrderFillEvent;
import io.github.yash_gadgil.tradingbot.core.event.StrategyEvent;
import io.github.yash_gadgil.tradingbot.core.eventbus.EventBus;
import io.github.yash_gadgil.tradingbot.core.order.OrderSide;
import io.github.yash_gadgil.tradingbot.core.order.OrderType;
import io.github.yash_gadgil.tradingbot.core.position.PositionBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Profile("!backtest")
public class RiskEngine implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(RiskEngine.class);

    private final AccountInfoService accountInfoService;
    private final EventBus eventBus;
    private final PositionBook positionBook;
    private final RiskProperties props;

    private final Map<String, Double> lastClose = new ConcurrentHashMap<>();

    private final Set<String> pendingEntries = ConcurrentHashMap.newKeySet();

    private volatile double realizedPnlToday = 0.0;
    private final AtomicBoolean killSwitchTripped = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    public RiskEngine(AccountInfoService accountInfoService, EventBus eventBus,
                      PositionBook positionBook, RiskProperties props) {
        this.accountInfoService = accountInfoService;
        this.eventBus = eventBus;
        this.positionBook = positionBook;
        this.props = props;
    }

    @Override
    public void start() {
        reconcile();
        eventBus.subscribe(CandleStickEvent.class, this::onCandle);
        eventBus.subscribe(OrderFillEvent.class, this::onFill);
        eventBus.subscribe(StrategyEvent.class, this::onSignal);
        running.set(true);
        logger.info("Risk Engine started - perOrderFraction={}, maxPosition/symbol={}, "
                        + "maxGrossExposure={}, dailyLossLimit=${}",
                props.getPerOrderFraction(), props.getMaxPositionPerSymbol(),
                props.getMaxGrossExposure(), props.getDailyLossLimit());
    }

    private void reconcile() {
        for (HeldPosition p : accountInfoService.getOpenPositions()) {
            positionBook.seed(p.symbol(), p.quantity(), p.avgEntryPrice());
            logger.info("Reconciled position {} {} @ {}", p.quantity(), p.symbol(), p.avgEntryPrice());
        }
    }

    private void onCandle(CandleStickEvent e) {
        lastClose.put(e.candleStick().symbol(), e.candleStick().close());
        evaluateKillSwitch();
    }

    void onFill(OrderFillEvent fill) {
        realizedPnlToday += positionBook.applyFill(fill);
        pendingEntries.remove(fill.instrument());
        evaluateKillSwitch();
    }

    void onSignal(StrategyEvent signal) {
        String symbol = signal.instrument();
        int position = positionBook.quantityOf(symbol);

        switch (signal.signalType()) {
            case ENTER_LONG -> tryEnter(signal, symbol, OrderSide.BUY, position);
            case ENTER_SHORT -> tryEnter(signal, symbol, OrderSide.SELL, position);
            case EXIT -> {
                if (position == 0) {
                    logger.debug("Dropping EXIT for {} - flat", symbol);
                    return;
                }
                publish(signal, symbol, position > 0 ? OrderSide.SELL : OrderSide.BUY, Math.abs(position));
            }
            case REDUCE -> {
                if (position == 0) {
                    logger.debug("Dropping REDUCE for {} - flat", symbol);
                    return;
                }
                int reduceBy = Math.abs(position) / 2;
                if (reduceBy == 0) {
                    logger.debug("Dropping REDUCE for {} - position too small to halve", symbol);
                    return;
                }
                publish(signal, symbol, position > 0 ? OrderSide.SELL : OrderSide.BUY, reduceBy);
            }
            case HOLD -> {  }
        }
    }

    private void tryEnter(StrategyEvent signal, String symbol, OrderSide side, int position) {
        if (position != 0 || pendingEntries.contains(symbol)) {
            logger.debug("Dropping {} for {} - already positioned/in-flight", signal.signalType(), symbol);
            return;
        }
        if (killSwitchTripped.get()) {
            logger.warn("BLOCK {} {} - kill switch is tripped", side, symbol);
            return;
        }

        Double price = lastClose.get(symbol);
        if (price == null || price <= 0) {
            logger.warn("Skipping {} {} - no price yet", side, symbol);
            return;
        }
        AccountSnapshot account = accountInfoService.getAccountSnapshot();
        if (account == null) {
            logger.warn("BLOCK {} {} - no account snapshot available", side, symbol);
            return;
        }
        double equity = account.totalBalance();

        int qty = (int) Math.floor((equity * props.getPerOrderFraction()) / price);
        if (qty < 1) {
            logger.warn("Skipping {} {} - sizing produced {} shares", side, symbol, qty);
            return;
        }

        if (qty > props.getMaxPositionPerSymbol()) {
            logger.warn("BLOCK {} {}x{} - exceeds per-symbol cap of {}",
                    side, symbol, qty, props.getMaxPositionPerSymbol());
            return;
        }

        double notional = qty * price;
        double projectedGross = positionBook.grossExposure(lastClose) + notional;
        if (projectedGross > props.getMaxGrossExposure() * equity) {
            logger.warn("BLOCK {} {}x{} - projected gross ${} exceeds cap ${}",
                    side, symbol, qty, projectedGross, props.getMaxGrossExposure() * equity);
            return;
        }

        if (notional > equity) {
            logger.warn("BLOCK {} {}x{} - notional ${} exceeds buying power ${}",
                    side, symbol, qty, notional, equity);
            return;
        }

        pendingEntries.add(symbol);
        publish(signal, symbol, side, qty);
    }

    private void publish(StrategyEvent signal, String symbol, OrderSide side, int qty) {
        OrderEvent order = new OrderEvent(
                UUID.randomUUID().toString(), signal.id(), symbol, side, qty,
                OrderType.MARKET, null, Instant.now());
        logger.info("RiskEngine -> {} {} x{} (from {} {})",
                side, symbol, qty, signal.id(), signal.signalType());
        eventBus.publish(order);
    }

    private void evaluateKillSwitch() {
        if (killSwitchTripped.get()) return;
        double dailyPnl = realizedPnlToday + positionBook.unrealizedPnl(lastClose);
        if (dailyPnl <= -props.getDailyLossLimit()) {
            killSwitchTripped.set(true);
            logger.error("KILL SWITCH TRIPPED - daily P&L ${} breached limit -${}. "
                    + "Blocking new entries.", String.format("%.2f", dailyPnl), props.getDailyLossLimit());
            if (props.isFlattenOnKill()) {
                flattenAll();
            }
        }
    }

    private void flattenAll() {
        for (PositionBook.Position p : positionBook.all().values()) {
            OrderSide side = p.quantity() > 0 ? OrderSide.SELL : OrderSide.BUY;
            OrderEvent order = new OrderEvent(
                    UUID.randomUUID().toString(), "kill-switch", p.symbol(), side,
                    Math.abs(p.quantity()), OrderType.MARKET, null, Instant.now());
            logger.warn("Kill switch flattening {} {} x{}", side, p.symbol(), Math.abs(p.quantity()));
            eventBus.publish(order);
        }
    }

    boolean isKillSwitchTripped() {
        return killSwitchTripped.get();
    }

    @Override
    public void stop() {
        running.set(false);
        logger.info("Risk Engine stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
