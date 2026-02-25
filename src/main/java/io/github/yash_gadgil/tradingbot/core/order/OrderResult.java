package io.github.yash_gadgil.tradingbot.core.order;

public record OrderResult(
        String clientOrderId,
        String brokerOrderId,
        Status status,
        Double filledAvgPrice,
        Integer filledQty,
        String message
) {
    public enum Status { ACCEPTED, FILLED, REJECTED, FAILED }

    public boolean isSuccess() {
        return status == Status.ACCEPTED || status == Status.FILLED;
    }

    public static OrderResult failed(String clientOrderId, String message) {
        return new OrderResult(clientOrderId, null, Status.FAILED, null, null, message);
    }

    public static OrderResult rejected(String clientOrderId, String brokerOrderId, String message) {
        return new OrderResult(clientOrderId, brokerOrderId, Status.REJECTED, null, null, message);
    }
}
