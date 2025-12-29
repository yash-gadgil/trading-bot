package io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata;

import io.github.yash_gadgil.tradingbot.core.marketdata.HistoricMarketDataProvider;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.model.Trade;
import net.jacobpeterson.alpaca.openapi.marketdata.model.StockBar;
import net.jacobpeterson.alpaca.openapi.marketdata.model.StockTrade;
import net.jacobpeterson.alpaca.rest.marketdata.AlpacaMarketDataAPI;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public class AlpacaHistoricMarketDataService implements HistoricMarketDataProvider {

    private final AlpacaMarketDataAPI marketDataAPI;

    public AlpacaHistoricMarketDataService(AlpacaMarketDataAPI marketDataAPI) {
        this.marketDataAPI = marketDataAPI;
    }

    @Override
    public List<CandleStick> getHistoricalCandleSticks(Set<String> symbols, OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            String symbolsStr = String.join(",", symbols);

            var response = marketDataAPI.stock().stockBars(
                    symbolsStr,
                    "1Min",
                    startTime,
                    endTime,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            if (!response.getBars().isEmpty()) {
                return response.getBars().entrySet().stream()
                    .flatMap(entry -> {
                        String symbol = entry.getKey();
                        List<StockBar> bars = entry.getValue();
                        return bars.stream().map(bar ->
                            new CandleStick(
                                    symbol,
                                    bar.getV(),
                                    bar.getH(),
                                    bar.getL(),
                                    bar.getO(),
                                    bar.getC()
                            )
                        );
                    })
                    .toList();
            } else {
                // ch
                System.out.println("no bars returned");
            }

        } catch (Exception e) {
            // ch
            System.err.println("api error: " + e.getMessage());
        }
        return List.of();
    }

    @Override
    public List<Trade> getHistoricalTrades(Set<String> symbols, OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            String symbolsStr = String.join(",", symbols);

            var response = marketDataAPI.stock().stockTrades(
                    symbolsStr,
                    startTime,
                    endTime,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            if (!response.getTrades().isEmpty()) {
                return response.getTrades().entrySet().stream()
                    .flatMap(entry -> {
                        String symbol = entry.getKey();
                        List<StockTrade> trades = entry.getValue();
                        return trades.stream().map(trade ->
                            new Trade(
                                symbol,
                                trade.getP(),
                                trade.getS()
                            )
                        );
                    })
                    .toList();
            } else {
                // ch
                System.out.println("no trades returned");
            }

        } catch (Exception e) {
            // ch
            System.err.println("api error: " + e.getMessage());
        }
        return List.of();
    }

}
