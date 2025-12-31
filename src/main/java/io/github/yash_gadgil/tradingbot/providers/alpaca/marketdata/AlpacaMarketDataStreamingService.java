package io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata;

import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.MarketDataEvent;
import io.github.yash_gadgil.tradingbot.core.event.TradeEvent;
import io.github.yash_gadgil.tradingbot.core.marketdata.MarketDataStreamingProvider;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.model.Trade;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.bar.StockBarMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.limituplimitdownband.StockLimitUpLimitDownBandMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.quote.StockQuoteMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.trade.StockTradeMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.tradecancelerror.StockTradeCancelErrorMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.tradecorrection.StockTradeCorrectionMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.tradingstatus.StockTradingStatusMessage;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataListener;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataWebsocket;

import java.util.Set;
import java.util.function.Consumer;

public class AlpacaMarketDataStreamingService implements MarketDataStreamingProvider {

    private final StockMarketDataWebsocket marketDataWebsocket;
    private final Set<String> symbols;

    private Consumer<MarketDataEvent> publishEvent;

    public AlpacaMarketDataStreamingService(StockMarketDataWebsocket marketDataWebsocket, Set<String> symbols) {
        marketDataWebsocket.setListener(new AlpacaStockMarketDataListener());
        this.marketDataWebsocket = marketDataWebsocket;
        this.symbols = symbols;
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

    @Override
    public void setEventPublisher(Consumer<MarketDataEvent> eventPublisher) {
        publishEvent = eventPublisher;
    }

    private class AlpacaStockMarketDataListener implements StockMarketDataListener {

        @Override
        public void onTrade(StockTradeMessage stockTradeMessage) {
                if (publishEvent != null) {
                publishEvent.accept(new TradeEvent(
                        stockTradeMessage.getTimestamp().toInstant(),
                        new Trade(
                            stockTradeMessage.getSymbol(),
                            stockTradeMessage.getPrice(),
                            stockTradeMessage.getSize()
                        )
                ));
            }
        }

        @Override
        public void onQuote(StockQuoteMessage stockQuoteMessage) {

        }

        @Override
        public void onMinuteBar(StockBarMessage stockBarMessage) {
            if (publishEvent != null) {
                publishEvent.accept(new CandleStickEvent(
                        stockBarMessage.getTimestamp().toInstant(),
                        new CandleStick(
                            stockBarMessage.getSymbol(),
                            stockBarMessage.getVolume(),
                            stockBarMessage.getHigh(),
                            stockBarMessage.getLow(),
                            stockBarMessage.getOpen(),
                            stockBarMessage.getClose()
                        )
                ));
            }
        }

        @Override
        public void onDailyBar(StockBarMessage stockBarMessage) {

        }

        @Override
        public void onUpdatedBar(StockBarMessage stockBarMessage) {

        }

        @Override
        public void onTradeCorrection(StockTradeCorrectionMessage stockTradeCorrectionMessage) {

        }

        @Override
        public void onTradeCancelError(StockTradeCancelErrorMessage stockTradeCancelErrorMessage) {

        }

        @Override
        public void onLimitUpLimitDownBand(StockLimitUpLimitDownBandMessage stockLimitUpLimitDownBandMessage) {

        }

        @Override
        public void onTradingStatus(StockTradingStatusMessage stockTradingStatusMessage) {

        }
    }

}
