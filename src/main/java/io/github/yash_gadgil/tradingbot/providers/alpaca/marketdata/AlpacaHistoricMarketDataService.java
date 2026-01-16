package io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata;

import io.github.yash_gadgil.tradingbot.core.marketdata.HistoricMarketDataProvider;
import io.github.yash_gadgil.tradingbot.core.marketdata.MarketDataException;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.model.Trade;
import net.jacobpeterson.alpaca.openapi.marketdata.model.StockBar;
import net.jacobpeterson.alpaca.openapi.marketdata.model.StockFeed;
import net.jacobpeterson.alpaca.rest.marketdata.AlpacaMarketDataAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AlpacaHistoricMarketDataService implements HistoricMarketDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(AlpacaHistoricMarketDataService.class);

    private final AlpacaMarketDataAPI marketDataAPI;

    public AlpacaHistoricMarketDataService(AlpacaMarketDataAPI marketDataAPI) {
        this.marketDataAPI = marketDataAPI;
    }

    @Override
    public List<CandleStick> getHistoricalCandleSticks(Set<String> symbols, OffsetDateTime startTime, OffsetDateTime endTime) {
        List<CandleStick> allCandles = new ArrayList<>();
        String symbolsStr = String.join(",", symbols);
        String pageToken = null;

        try {
            do {
                var response = marketDataAPI.stock().stockBars(
                        symbolsStr, "1Min", startTime, endTime, 10000L,
                        null, null, StockFeed.IEX, null, pageToken, null);

                if (response.getBars() != null && !response.getBars().isEmpty()) {
                    response.getBars().forEach((symbol, bars) -> {
                        for (StockBar bar : bars) {
                            allCandles.add(new CandleStick(
                                    bar.getT().toInstant(), symbol, bar.getV(),
                                    bar.getH(), bar.getL(), bar.getO(), bar.getC()));
                        }
                    });
                }

                pageToken = response.getNextPageToken();
            } while (pageToken != null);

        } catch (Exception e) {

            throw new MarketDataException("Failed to fetch historical candles for " + symbolsStr, e);
        }

        return allCandles;
    }

    @Override
    public List<Trade> getHistoricalTrades(Set<String> symbols, OffsetDateTime startTime, OffsetDateTime endTime) {
        String symbolsStr = String.join(",", symbols);
        try {
            var response = marketDataAPI.stock().stockTrades(
                    symbolsStr, startTime, endTime, null, null, null, null, null, null);

            if (response.getTrades() == null || response.getTrades().isEmpty()) {
                logger.info("No historical trades returned for {}", symbolsStr);
                return List.of();
            }
            return response.getTrades().entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream()
                            .map(trade -> new Trade(entry.getKey(), trade.getP(), trade.getS())))
                    .toList();

        } catch (Exception e) {
            throw new MarketDataException("Failed to fetch historical trades for " + symbolsStr, e);
        }
    }
}
