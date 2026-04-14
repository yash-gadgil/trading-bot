package io.github.yash_gadgil.tradingbot.testutil;

import io.github.yash_gadgil.tradingbot.core.event.OrderEvent;
import io.github.yash_gadgil.tradingbot.core.order.IExecutionService;
import io.github.yash_gadgil.tradingbot.core.order.OrderResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeBroker implements IExecutionService {

    public final List<OrderEvent> placed = new ArrayList<>();
    public final Map<String, Double> fillPrices = new HashMap<>();
    public double defaultFillPrice = 100.0;
    public boolean rejectAll = false;

    @Override
    public OrderResult placeOrder(OrderEvent order) {
        placed.add(order);
        if (rejectAll) {
            return OrderResult.rejected(order.clientOrderId(), null, "rejected by fake");
        }
        double px = fillPrices.getOrDefault(order.instrument(), defaultFillPrice);
        return new OrderResult(order.clientOrderId(), "brk-" + placed.size(),
                OrderResult.Status.FILLED, px, order.quantity(), "filled");
    }

    @Override
    public boolean cancelOrder(String brokerOrderId) {
        return true;
    }
}
