package io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata;

import io.github.yash_gadgil.tradingbot.core.marketdata.MarketDataStreamingService;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataWebsocket;

import java.util.Set;

public class AlpacaMarketDataStreamingService implements MarketDataStreamingService {

    private final StockMarketDataWebsocket marketDataWebsocket;
    private final Set<String> symbols;

    public AlpacaMarketDataStreamingService(StockMarketDataWebsocket marketDataWebsocket, Set<String> symbols) {
        marketDataWebsocket.setListener(new AlpacaStockMarketDataListener());
        this.marketDataWebsocket = marketDataWebsocket;
        this.symbols = symbols;
    }

    public Set<String> getSymbols() {
        return symbols;
    }

    @Override
    public void connect() {
        marketDataWebsocket.connect();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        marketDataWebsocket.setTradeSubscriptions(symbols);
        marketDataWebsocket.setMinuteBarSubscriptions(symbols);
    }

    @Override
    public void disconnect() {
        marketDataWebsocket.disconnect();
    }

}
