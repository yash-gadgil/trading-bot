package io.github.yash_gadgil.tradingbot.testutil;

import io.github.yash_gadgil.tradingbot.core.marketdata.HistoricMarketDataProvider;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.model.Trade;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public class FixedHistoricProvider implements HistoricMarketDataProvider {

    private final List<CandleStick> candles;

    public FixedHistoricProvider(List<CandleStick> candles) {
        this.candles = candles;
    }

    @Override
    public List<CandleStick> getHistoricalCandleSticks(Set<String> symbols, OffsetDateTime startTime, OffsetDateTime endTime) {
        return candles;
    }

    @Override
    public List<Trade> getHistoricalTrades(Set<String> symbols, OffsetDateTime startTime, OffsetDateTime endTime) {
        return List.of();
    }
}
