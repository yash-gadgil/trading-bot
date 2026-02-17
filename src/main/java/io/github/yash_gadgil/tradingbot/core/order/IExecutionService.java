package io.github.yash_gadgil.tradingbot.core.order;

import io.github.yash_gadgil.tradingbot.core.event.OrderEvent;

public interface IExecutionService {

    OrderResult placeOrder(OrderEvent order);

    boolean cancelOrder(String brokerOrderId);
}
