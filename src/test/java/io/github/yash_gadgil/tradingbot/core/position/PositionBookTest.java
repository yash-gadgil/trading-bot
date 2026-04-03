package io.github.yash_gadgil.tradingbot.core.position;

import io.github.yash_gadgil.tradingbot.core.event.OrderFillEvent;
import io.github.yash_gadgil.tradingbot.core.order.OrderSide;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PositionBookTest {

    private static OrderFillEvent fill(String sym, OrderSide side, int qty, double price) {
        return new OrderFillEvent("cid", "strat", sym, side, qty, price, Instant.now());
    }

    @Test
    void buildsLongPositionFromBuy() {
        PositionBook book = new PositionBook();
        book.applyFill(fill("AAPL", OrderSide.BUY, 10, 100.0));
        assertEquals(10, book.quantityOf("AAPL"));
        assertEquals(100.0, book.get("AAPL").orElseThrow().avgEntryPrice());
    }

    @Test
    void buildsShortPositionFromSell() {
        PositionBook book = new PositionBook();
        book.applyFill(fill("AAPL", OrderSide.SELL, 5, 200.0));
        assertEquals(-5, book.quantityOf("AAPL"));
    }

    @Test
    void volumeWeightsAverageEntryWhenAdding() {
        PositionBook book = new PositionBook();
        book.applyFill(fill("AAPL", OrderSide.BUY, 10, 100.0));
        book.applyFill(fill("AAPL", OrderSide.BUY, 10, 120.0));
        assertEquals(20, book.quantityOf("AAPL"));
        assertEquals(110.0, book.get("AAPL").orElseThrow().avgEntryPrice(), 1e-9);
    }

    @Test
    void partialCloseRealizesPnlAndKeepsAverage() {
        PositionBook book = new PositionBook();
        book.applyFill(fill("AAPL", OrderSide.BUY, 10, 100.0));
        double realized = book.applyFill(fill("AAPL", OrderSide.SELL, 4, 110.0));
        assertEquals(40.0, realized, 1e-9);
        assertEquals(6, book.quantityOf("AAPL"));
        assertEquals(100.0, book.get("AAPL").orElseThrow().avgEntryPrice(), 1e-9);
    }

    @Test
    void fullCloseRemovesPositionAndRealizesPnl() {
        PositionBook book = new PositionBook();
        book.applyFill(fill("AAPL", OrderSide.BUY, 10, 100.0));
        double realized = book.applyFill(fill("AAPL", OrderSide.SELL, 10, 90.0));
        assertEquals(-100.0, realized, 1e-9);
        assertEquals(0, book.quantityOf("AAPL"));
        assertTrue(book.get("AAPL").isEmpty());
    }

    @Test
    void oversizedSellFlipsLongToShortAtFillPrice() {
        PositionBook book = new PositionBook();
        book.applyFill(fill("AAPL", OrderSide.BUY, 10, 100.0));
        double realized = book.applyFill(fill("AAPL", OrderSide.SELL, 15, 120.0));
        assertEquals(200.0, realized, 1e-9);
        assertEquals(-5, book.quantityOf("AAPL"));
        assertEquals(120.0, book.get("AAPL").orElseThrow().avgEntryPrice(), 1e-9);
    }

    @Test
    void shortThenCoverRealizesPnl() {
        PositionBook book = new PositionBook();
        book.applyFill(fill("AAPL", OrderSide.SELL, 10, 100.0));
        double realized = book.applyFill(fill("AAPL", OrderSide.BUY, 10, 90.0));
        assertEquals(100.0, realized, 1e-9);
        assertEquals(0, book.quantityOf("AAPL"));
    }

    @Test
    void grossExposureSumsAbsoluteMarketValue() {
        PositionBook book = new PositionBook();
        book.applyFill(fill("AAPL", OrderSide.BUY, 10, 100.0));
        book.applyFill(fill("MSFT", OrderSide.SELL, 5, 200.0));
        double gross = book.grossExposure(Map.of("AAPL", 110.0, "MSFT", 190.0));
        assertEquals(10 * 110.0 + 5 * 190.0, gross, 1e-9);
    }

    @Test
    void unrealizedPnlMarksToMarket() {
        PositionBook book = new PositionBook();
        book.applyFill(fill("AAPL", OrderSide.BUY, 10, 100.0));
        book.applyFill(fill("MSFT", OrderSide.SELL, 5, 200.0));

        double pnl = book.unrealizedPnl(Map.of("AAPL", 105.0, "MSFT", 190.0));
        assertEquals(100.0, pnl, 1e-9);
    }

    @Test
    void seedSetsAndClearsPositions() {
        PositionBook book = new PositionBook();
        book.seed("AAPL", 7, 150.0);
        assertEquals(7, book.quantityOf("AAPL"));
        book.seed("AAPL", 0, 0.0);
        assertEquals(0, book.quantityOf("AAPL"));
    }
}
