package io.github.yash_gadgil.tradingbot.core.marketdata;

import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.model.Trade;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public interface HistoricMarketDataProvider {

    List<CandleStick> getHistoricalCandleSticks(Set<String> symbols, OffsetDateTime startTime, OffsetDateTime endTime);

    List<Trade> getHistoricalTrades(Set<String> symbols, OffsetDateTime startTime, OffsetDateTime endTime);

}
