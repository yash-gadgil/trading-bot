package io.github.yash_gadgil.tradingbot.core.event;

import io.github.yash_gadgil.tradingbot.core.eventbus.Event;
import io.github.yash_gadgil.tradingbot.core.order.OrderSide;

import java.time.Instant;

public record OrderFillEvent(
        String clientOrderId,
        String strategyId,
        String instrument,
        OrderSide side,
        int quantity,
        double fillPrice,
        Instant timestamp
) implements Event {}
