package io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata;

import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.bar.StockBarMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.limituplimitdownband.StockLimitUpLimitDownBandMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.quote.StockQuoteMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.trade.StockTradeMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.tradecancelerror.StockTradeCancelErrorMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.tradecorrection.StockTradeCorrectionMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.tradingstatus.StockTradingStatusMessage;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataListener;

public class AlpacaStockMarketDataListener implements StockMarketDataListener {

    @Override
    public void onTrade(StockTradeMessage stockTradeMessage) {
        System.out.printf("Symbol: %s Price: %f", stockTradeMessage.getSymbol(), stockTradeMessage.getPrice());
    }

    @Override
    public void onQuote(StockQuoteMessage stockQuoteMessage) {

    }

    @Override
    public void onMinuteBar(StockBarMessage stockBarMessage) {
        System.out.printf("Symbol: %s Volume: %d", stockBarMessage.getSymbol(), stockBarMessage.getVolume());
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
