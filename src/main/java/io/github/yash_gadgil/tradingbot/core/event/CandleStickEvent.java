package io.github.yash_gadgil.tradingbot.core.event;

public record CandleStickEvent(
       String symbol,
       Long volume,
       Double high,
       Double low,
       Double open,
       Double close
) implements MarketDataEvent {}
