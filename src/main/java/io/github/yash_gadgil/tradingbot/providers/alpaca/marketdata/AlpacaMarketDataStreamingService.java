package io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata;

import io.github.yash_gadgil.tradingbot.core.event.CandleStickEvent;
import io.github.yash_gadgil.tradingbot.core.event.MarketDataEvent;
import io.github.yash_gadgil.tradingbot.core.event.TradeEvent;
import io.github.yash_gadgil.tradingbot.core.marketdata.MarketDataStreamingProvider;
import io.github.yash_gadgil.tradingbot.core.marketdata.ReconnectPolicy;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import io.github.yash_gadgil.tradingbot.core.model.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class AlpacaMarketDataStreamingService implements MarketDataStreamingProvider {

    private static final Logger logger = LoggerFactory.getLogger(AlpacaMarketDataStreamingService.class);

    private final StockMarketDataWebsocket marketDataWebsocket;
    private final Set<String> symbols;
    private final ReconnectPolicy reconnectPolicy = new ReconnectPolicy(1_000, 30_000, 2.0);

    private volatile boolean stopRequested = false;
    private Consumer<MarketDataEvent> publishEvent;

    public AlpacaMarketDataStreamingService(StockMarketDataWebsocket marketDataWebsocket, Set<String> symbols) {
        marketDataWebsocket.setListener(new AlpacaStockMarketDataListener());
        this.marketDataWebsocket = marketDataWebsocket;
        this.symbols = symbols;
    }

    @Override
    public void connect() {
        stopRequested = false;
        while (!stopRequested) {
            try {
                attemptConnect();
                reconnectPolicy.reset();
                logger.info("Market data stream connected and subscribed to {}", symbols);
                return;
            } catch (RuntimeException e) {
                if (stopRequested) return;
                long delay = reconnectPolicy.nextDelayMs();
                logger.warn("Market data connect attempt {} failed ({}); retrying in {} ms",
                        reconnectPolicy.attempts(), e.getMessage(), delay);
                if (!sleep(delay)) return;
            }
        }
    }

    private void attemptConnect() {
        marketDataWebsocket.connect();
        try {
            Boolean authorized = marketDataWebsocket.getAuthorizationFuture().get(15, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(authorized)) {
                throw new IllegalStateException("Alpaca market data websocket failed to authenticate");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Alpaca websocket authentication", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Alpaca market data websocket did not authenticate in time", e);
        }

        marketDataWebsocket.setTradeSubscriptions(symbols);
        marketDataWebsocket.setMinuteBarSubscriptions(symbols);
    }

    private boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void disconnect() {
        stopRequested = true;
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
                            stockBarMessage.getTimestamp().toInstant(),
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
