package io.github.yash_gadgil.tradingbot.core.event;

import io.github.yash_gadgil.tradingbot.core.eventbus.Event;
import io.github.yash_gadgil.tradingbot.core.order.OrderSide;
import io.github.yash_gadgil.tradingbot.core.order.OrderType;

import java.time.Instant;

public record OrderEvent(
        String clientOrderId,
        String strategyId,
        String instrument,
        OrderSide side,
        int quantity,
        OrderType orderType,
        Double limitPrice,
        Instant timestamp
) implements Event {}
