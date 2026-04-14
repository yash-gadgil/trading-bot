package io.github.yash_gadgil.tradingbot.core.position;

import io.github.yash_gadgil.tradingbot.core.event.OrderFillEvent;
import io.github.yash_gadgil.tradingbot.core.order.OrderSide;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PositionBook {

    public record Position(String symbol, int quantity, double avgEntryPrice) {}

    private final Map<String, Position> positions = new ConcurrentHashMap<>();

    public void seed(String symbol, int quantity, double avgEntryPrice) {
        if (quantity == 0) {
            positions.remove(symbol);
        } else {
            positions.put(symbol, new Position(symbol, quantity, avgEntryPrice));
        }
    }

    public double applyFill(OrderFillEvent fill) {
        int fillSigned = fill.side() == OrderSide.BUY ? fill.quantity() : -fill.quantity();
        double price = fill.fillPrice();
        Position current = positions.get(fill.instrument());

        if (current == null || current.quantity() == 0) {
            put(fill.instrument(), fillSigned, price);
            return 0.0;
        }

        int cur = current.quantity();
        double avg = current.avgEntryPrice();

        boolean sameDirection = (cur > 0) == (fillSigned > 0);
        if (sameDirection) {

            int newQty = cur + fillSigned;
            double newAvg = (Math.abs(cur) * avg + Math.abs(fillSigned) * price) / Math.abs(newQty);
            put(fill.instrument(), newQty, newAvg);
            return 0.0;
        }

        int closedQty = Math.min(Math.abs(cur), Math.abs(fillSigned));
        double realized = cur > 0
                ? closedQty * (price - avg)
                : closedQty * (avg - price);

        int newQty = cur + fillSigned;
        if (newQty == 0) {
            positions.remove(fill.instrument());
        } else if ((newQty > 0) == (cur > 0)) {

            put(fill.instrument(), newQty, avg);
        } else {

            put(fill.instrument(), newQty, price);
        }
        return realized;
    }

    private void put(String symbol, int qty, double avg) {
        positions.put(symbol, new Position(symbol, qty, avg));
    }

    public Optional<Position> get(String symbol) {
        return Optional.ofNullable(positions.get(symbol));
    }

    public int quantityOf(String symbol) {
        Position p = positions.get(symbol);
        return p == null ? 0 : p.quantity();
    }

    public Map<String, Position> all() {
        return Map.copyOf(positions);
    }

    public double grossExposure(Map<String, Double> lastPrices) {
        double sum = 0.0;
        for (Position p : positions.values()) {
            double price = lastPrices.getOrDefault(p.symbol(), p.avgEntryPrice());
            sum += Math.abs(p.quantity()) * price;
        }
        return sum;
    }

    public double unrealizedPnl(Map<String, Double> lastPrices) {
        double sum = 0.0;
        for (Position p : positions.values()) {
            double price = lastPrices.getOrDefault(p.symbol(), p.avgEntryPrice());
            sum += p.quantity() * (price - p.avgEntryPrice());
        }
        return sum;
    }
}
