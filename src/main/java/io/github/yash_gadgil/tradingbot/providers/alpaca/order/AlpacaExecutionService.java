package io.github.yash_gadgil.tradingbot.providers.alpaca.order;

import io.github.yash_gadgil.tradingbot.core.event.OrderEvent;
import io.github.yash_gadgil.tradingbot.core.order.IExecutionService;
import io.github.yash_gadgil.tradingbot.core.order.OrderResult;
import net.jacobpeterson.alpaca.openapi.trader.ApiException;
import net.jacobpeterson.alpaca.openapi.trader.model.Order;
import net.jacobpeterson.alpaca.openapi.trader.model.OrderSide;
import net.jacobpeterson.alpaca.openapi.trader.model.OrderStatus;
import net.jacobpeterson.alpaca.openapi.trader.model.OrderType;
import net.jacobpeterson.alpaca.openapi.trader.model.PostOrderRequest;
import net.jacobpeterson.alpaca.openapi.trader.model.TimeInForce;
import net.jacobpeterson.alpaca.rest.trader.AlpacaTraderAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AlpacaExecutionService implements IExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(AlpacaExecutionService.class);

    private final AlpacaTraderAPI alpacaTraderAPI;

    public AlpacaExecutionService(AlpacaTraderAPI alpacaTraderAPI) {
        this.alpacaTraderAPI = alpacaTraderAPI;
    }

    @Override
    public OrderResult placeOrder(OrderEvent order) {
        try {
            PostOrderRequest request = new PostOrderRequest()
                    .symbol(order.instrument())
                    .qty(Integer.toString(order.quantity()))
                    .side(order.side() == io.github.yash_gadgil.tradingbot.core.order.OrderSide.BUY
                            ? OrderSide.BUY : OrderSide.SELL)
                    .type(order.orderType() == io.github.yash_gadgil.tradingbot.core.order.OrderType.LIMIT
                            ? OrderType.LIMIT : OrderType.MARKET)
                    .timeInForce(TimeInForce.DAY)
                    .clientOrderId(order.clientOrderId());

            if (order.orderType() == io.github.yash_gadgil.tradingbot.core.order.OrderType.LIMIT
                    && order.limitPrice() != null) {
                request.limitPrice(Double.toString(order.limitPrice()));
            }

            Order response = alpacaTraderAPI.orders().postOrder(request);
            return mapResponse(order.clientOrderId(), response);

        } catch (ApiException e) {

            String detail = e.getResponseBody() != null ? e.getResponseBody() : e.getMessage();
            if (e.getCode() == 422) {
                logger.warn("Alpaca rejected order {} ({}): {}", order.clientOrderId(), order.instrument(), detail);
                return OrderResult.rejected(order.clientOrderId(), null, detail);
            }
            logger.error("Alpaca order {} failed ({}): code={} {}",
                    order.clientOrderId(), order.instrument(), e.getCode(), detail);
            return OrderResult.failed(order.clientOrderId(), detail);
        } catch (RuntimeException e) {
            logger.error("Unexpected error placing order {} ({}): {}",
                    order.clientOrderId(), order.instrument(), e.getMessage(), e);
            return OrderResult.failed(order.clientOrderId(), e.getMessage());
        }
    }

    @Override
    public boolean cancelOrder(String brokerOrderId) {
        try {
            alpacaTraderAPI.orders().deleteOrderByOrderID(UUID.fromString(brokerOrderId));
            return true;
        } catch (ApiException | IllegalArgumentException e) {
            logger.warn("Failed to cancel order {}: {}", brokerOrderId, e.getMessage());
            return false;
        }
    }

    private static OrderResult mapResponse(String clientOrderId, Order response) {
        OrderStatus status = response.getStatus();
        Double filledAvgPrice = parseDouble(response.getFilledAvgPrice());
        Integer filledQty = parseInt(response.getFilledQty());

        OrderResult.Status mapped = switch (status) {
            case FILLED -> OrderResult.Status.FILLED;
            case REJECTED, CANCELED, EXPIRED, STOPPED, SUSPENDED -> OrderResult.Status.REJECTED;
            default -> OrderResult.Status.ACCEPTED;
        };

        return new OrderResult(
                clientOrderId,
                response.getId(),
                mapped,
                filledAvgPrice,
                filledQty,
                status != null ? status.getValue() : null
        );
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
