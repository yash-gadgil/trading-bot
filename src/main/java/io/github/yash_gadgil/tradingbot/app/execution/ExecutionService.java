package io.github.yash_gadgil.tradingbot.app.execution;

import io.github.yash_gadgil.tradingbot.core.event.OrderEvent;
import io.github.yash_gadgil.tradingbot.core.event.OrderFillEvent;
import io.github.yash_gadgil.tradingbot.core.eventbus.EventBus;
import io.github.yash_gadgil.tradingbot.core.order.IExecutionService;
import io.github.yash_gadgil.tradingbot.core.order.OrderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Profile("!backtest")
public class ExecutionService implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

    private final EventBus eventBus;
    private final IExecutionService broker;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ExecutionService(EventBus eventBus, IExecutionService broker) {
        this.eventBus = eventBus;
        this.broker = broker;
    }

    @Override
    public void start() {
        eventBus.subscribe(OrderEvent.class, this::onOrder);
        running.set(true);
        logger.info("Execution Service started");
    }

    void onOrder(OrderEvent order) {
        try {
            OrderResult result = broker.placeOrder(order);

            switch (result.status()) {
                case FILLED, ACCEPTED -> {
                    logger.info("Order {} {} {}x{} -> {} (broker={})",
                            order.clientOrderId(), order.side(), order.quantity(),
                            order.instrument(), result.status(), result.brokerOrderId());
                    publishFillIfPriced(order, result);
                }
                case REJECTED -> logger.error("Order {} for {} REJECTED: {}",
                        order.clientOrderId(), order.instrument(), result.message());
                case FAILED -> logger.error("Order {} for {} FAILED: {}",
                        order.clientOrderId(), order.instrument(), result.message());
            }
        } catch (RuntimeException e) {

            logger.error("Order {} for {} threw out of the broker adapter: {}",
                    order.clientOrderId(), order.instrument(), e.getMessage(), e);
        }
    }

    private void publishFillIfPriced(OrderEvent order, OrderResult result) {
        Double price = result.filledAvgPrice();
        if (price == null && order.limitPrice() != null) {
            price = order.limitPrice();
        }
        if (price == null) {

            logger.info("Order {} accepted, awaiting fill price", order.clientOrderId());
            return;
        }
        int qty = result.filledQty() != null ? result.filledQty() : order.quantity();
        eventBus.publish(new OrderFillEvent(
                order.clientOrderId(), order.strategyId(), order.instrument(),
                order.side(), qty, price, Instant.now()
        ));
    }

    @Override
    public void stop() {
        running.set(false);
        logger.info("Execution Service stopped");
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
