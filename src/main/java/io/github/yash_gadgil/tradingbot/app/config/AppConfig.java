package io.github.yash_gadgil.tradingbot.app.config;

import io.github.yash_gadgil.tradingbot.core.account.AccountInfoService;
import io.github.yash_gadgil.tradingbot.core.eventbus.EventBus;
import io.github.yash_gadgil.tradingbot.core.eventbus.InMemoryEventBus;
import io.github.yash_gadgil.tradingbot.core.marketdata.HistoricMarketDataProvider;
import io.github.yash_gadgil.tradingbot.core.marketdata.MarketDataStreamingProvider;
import io.github.yash_gadgil.tradingbot.providers.alpaca.account.AlpacaAccountService;
import io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata.AlpacaHistoricMarketDataService;
import io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata.AlpacaMarketDataStreamingService;
import io.github.cdimascio.dotenv.Dotenv;
import net.jacobpeterson.alpaca.model.util.apitype.MarketDataWebsocketSourceType;
import net.jacobpeterson.alpaca.model.util.apitype.TraderAPIEndpointType;
import net.jacobpeterson.alpaca.rest.marketdata.AlpacaMarketDataAPI;
import net.jacobpeterson.alpaca.rest.trader.AlpacaTraderAPI;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataWebsocket;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class AppConfig {

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();
    }

    @Bean
    public OkHttpClient client() {
        return new OkHttpClient();
    }

    @Bean
    public EventBus eventBus() {
        return new InMemoryEventBus();
    }

    @Bean
    public AlpacaTraderAPI alpacaTraderAPI(Dotenv dotenv, OkHttpClient client) {
        String apiKey = dotenv.get("APCA_API_KEY_ID");
        String apiSecret = dotenv.get("APCA_API_SECRET_KEY");

        if (apiKey == null || apiSecret == null) {
            throw new IllegalStateException("env variables not set");
        }

        return new AlpacaTraderAPI(apiKey, apiSecret, null, TraderAPIEndpointType.PAPER, client);
    }

    @Bean
    public AccountInfoService accountInfoService(AlpacaTraderAPI alpacaTraderAPI) {
        return new AlpacaAccountService(alpacaTraderAPI);
    }

    @Bean
    public StockMarketDataWebsocket stockMarketDataWebsocket(OkHttpClient client, Dotenv dotenv) {
        String apiKey = dotenv.get("APCA_API_KEY_ID");
        String apiSecret = dotenv.get("APCA_API_SECRET_KEY");

        if (apiKey == null || apiSecret == null) {
            throw new IllegalStateException("env variables not set");
        }

        return new StockMarketDataWebsocket(
                client, apiKey, apiSecret, null, null, MarketDataWebsocketSourceType.IEX
        );
    }

    @Bean
    public Set<String> tradingSymbols() {
        return Set.of("AAPL", "GOOGL", "MSFT");
    }

    @Bean
    public AlpacaMarketDataAPI alpacaMarketDataAPI(Dotenv dotenv, OkHttpClient client) {

        String apiKey = dotenv.get("APCA_API_KEY_ID");
        String apiSecret = dotenv.get("APCA_API_SECRET_KEY");

        if (apiKey == null || apiSecret == null) {
            throw new IllegalStateException("env variables not set");
        }
        return new AlpacaMarketDataAPI(
            apiKey,
            apiSecret,
            null,
            null,
            client
        );
    }

    @Bean
    public MarketDataStreamingProvider marketDataStreamingServiceInterface(
            StockMarketDataWebsocket stockMarketDataWebsocket,
            Set<String> tradingSymbols) {
        return new AlpacaMarketDataStreamingService(stockMarketDataWebsocket, tradingSymbols);
    }

    @Bean
    public HistoricMarketDataProvider historicMarketDataProvider(AlpacaMarketDataAPI alpacaMarketDataAPI) {
        return new AlpacaHistoricMarketDataService(alpacaMarketDataAPI);
    }
}
