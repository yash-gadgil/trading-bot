package io.github.yash_gadgil.tradingbot.providers.alpaca.order;

import io.github.yash_gadgil.tradingbot.core.event.OrderEvent;
import io.github.yash_gadgil.tradingbot.core.order.OrderResult;
import io.github.yash_gadgil.tradingbot.core.order.OrderSide;
import io.github.yash_gadgil.tradingbot.core.order.OrderType;
import net.jacobpeterson.alpaca.openapi.trader.ApiException;
import net.jacobpeterson.alpaca.openapi.trader.api.OrdersApi;
import net.jacobpeterson.alpaca.openapi.trader.model.Order;
import net.jacobpeterson.alpaca.openapi.trader.model.OrderStatus;
import net.jacobpeterson.alpaca.openapi.trader.model.PostOrderRequest;
import net.jacobpeterson.alpaca.openapi.trader.model.TimeInForce;
import net.jacobpeterson.alpaca.rest.trader.AlpacaTraderAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlpacaExecutionServiceTest {

    private AlpacaTraderAPI api;
    private OrdersApi ordersApi;
    private AlpacaExecutionService service;

    @BeforeEach
    void setUp() {
        api = mock(AlpacaTraderAPI.class);
        ordersApi = mock(OrdersApi.class);
        when(api.orders()).thenReturn(ordersApi);
        service = new AlpacaExecutionService(api);
    }

    private OrderEvent buy(int qty) {
        return new OrderEvent("cid-7", "strat", "AAPL", OrderSide.BUY, qty,
                OrderType.MARKET, null, Instant.now());
    }

    @Test
    void mapsOrderEventToAlpacaRequestFields() throws Exception {
        Order filled = mock(Order.class);
        when(filled.getStatus()).thenReturn(OrderStatus.FILLED);
        when(filled.getId()).thenReturn("brk-9");
        when(filled.getFilledAvgPrice()).thenReturn("150.25");
        when(filled.getFilledQty()).thenReturn("10");
        when(ordersApi.postOrder(any())).thenReturn(filled);

        OrderResult result = service.placeOrder(buy(10));

        ArgumentCaptor<PostOrderRequest> captor = ArgumentCaptor.forClass(PostOrderRequest.class);
        verify(ordersApi).postOrder(captor.capture());
        PostOrderRequest req = captor.getValue();

        assertEquals("AAPL", req.getSymbol());
        assertEquals("10", req.getQty());
        assertEquals(net.jacobpeterson.alpaca.openapi.trader.model.OrderSide.BUY, req.getSide());
        assertEquals(net.jacobpeterson.alpaca.openapi.trader.model.OrderType.MARKET, req.getType());
        assertEquals(TimeInForce.DAY, req.getTimeInForce());
        assertEquals("cid-7", req.getClientOrderId());

        assertEquals(OrderResult.Status.FILLED, result.status());
        assertEquals("brk-9", result.brokerOrderId());
        assertEquals(150.25, result.filledAvgPrice());
        assertEquals(10, result.filledQty());
    }

    @Test
    void limitOrderSetsLimitPrice() throws Exception {
        Order accepted = mock(Order.class);
        when(accepted.getStatus()).thenReturn(OrderStatus.ACCEPTED);
        when(accepted.getId()).thenReturn("brk-1");
        when(ordersApi.postOrder(any())).thenReturn(accepted);

        OrderEvent limit = new OrderEvent("cid-l", "strat", "MSFT", OrderSide.SELL, 3,
                OrderType.LIMIT, 321.5, Instant.now());
        service.placeOrder(limit);

        ArgumentCaptor<PostOrderRequest> captor = ArgumentCaptor.forClass(PostOrderRequest.class);
        verify(ordersApi).postOrder(captor.capture());
        PostOrderRequest req = captor.getValue();
        assertEquals(net.jacobpeterson.alpaca.openapi.trader.model.OrderType.LIMIT, req.getType());
        assertEquals(net.jacobpeterson.alpaca.openapi.trader.model.OrderSide.SELL, req.getSide());
        assertEquals("321.5", req.getLimitPrice());
    }

    @Test
    void apiException_returnsFailed() throws Exception {
        when(ordersApi.postOrder(any())).thenThrow(new ApiException(500, "internal error"));

        OrderResult result = service.placeOrder(buy(10));

        assertEquals(OrderResult.Status.FAILED, result.status());
        assertNull(result.brokerOrderId());
        assertEquals("cid-7", result.clientOrderId());
    }

    @Test
    void businessRejection_422_returnsRejected() throws Exception {
        when(ordersApi.postOrder(any())).thenThrow(new ApiException(422, "insufficient buying power"));

        OrderResult result = service.placeOrder(buy(10_000));

        assertEquals(OrderResult.Status.REJECTED, result.status());
        assertEquals("cid-7", result.clientOrderId());
    }

    @Test
    void rejectedStatus_mapsToRejected() throws Exception {
        Order rejected = mock(Order.class);
        when(rejected.getStatus()).thenReturn(OrderStatus.REJECTED);
        when(rejected.getId()).thenReturn("brk-2");
        when(ordersApi.postOrder(any())).thenReturn(rejected);

        OrderResult result = service.placeOrder(buy(10));
        assertEquals(OrderResult.Status.REJECTED, result.status());
    }

    @Test
    void cancelOrder_delegatesToApi() throws Exception {
        java.util.UUID id = java.util.UUID.randomUUID();
        assertTrue(service.cancelOrder(id.toString()));
        verify(ordersApi).deleteOrderByOrderID(id);
    }

    @Test
    void cancelOrder_invalidId_returnsFalse() {
        assertFalse(service.cancelOrder("not-a-uuid"));
    }
}
