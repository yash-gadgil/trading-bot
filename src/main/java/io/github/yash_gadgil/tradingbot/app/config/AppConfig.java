package io.github.yash_gadgil.tradingbot.app.config;

import io.github.yash_gadgil.tradingbot.core.account.AccountInfoService;
import io.github.yash_gadgil.tradingbot.core.eventbus.EventBus;
import io.github.yash_gadgil.tradingbot.core.eventbus.InMemoryEventBus;
import io.github.yash_gadgil.tradingbot.core.marketdata.HistoricMarketDataProvider;
import io.github.yash_gadgil.tradingbot.core.marketdata.MarketDataStreamingProvider;
import io.github.yash_gadgil.tradingbot.app.risk.RiskProperties;
import io.github.yash_gadgil.tradingbot.core.order.IExecutionService;
import io.github.yash_gadgil.tradingbot.core.position.PositionBook;
import io.github.yash_gadgil.tradingbot.core.strategy.FadeTheMoveStrategy;
import io.github.yash_gadgil.tradingbot.core.strategy.PpoStrategy;
import io.github.yash_gadgil.tradingbot.core.strategy.Strategy;
import io.github.yash_gadgil.tradingbot.providers.alpaca.account.AlpacaAccountService;
import io.github.yash_gadgil.tradingbot.providers.alpaca.order.AlpacaExecutionService;
import io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata.AlpacaHistoricMarketDataService;
import io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata.AlpacaMarketDataStreamingService;
import io.github.cdimascio.dotenv.Dotenv;
import net.jacobpeterson.alpaca.model.util.apitype.MarketDataWebsocketSourceType;
import net.jacobpeterson.alpaca.model.util.apitype.TraderAPIEndpointType;
import net.jacobpeterson.alpaca.rest.marketdata.AlpacaMarketDataAPI;
import net.jacobpeterson.alpaca.rest.trader.AlpacaTraderAPI;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataWebsocket;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Set;

@Configuration
@EnableConfigurationProperties({RiskProperties.class, StrategyProperties.class})
public class AppConfig {

    @Bean
    public PositionBook positionBook() {
        return new PositionBook();
    }

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

    @Bean(destroyMethod = "shutdown")
    public EventBus eventBus() {
        return new InMemoryEventBus();
    }

    @Bean
    @Profile("!backtest")
    public AlpacaTraderAPI alpacaTraderAPI(Dotenv dotenv, OkHttpClient client) {
        String apiKey = dotenv.get("APCA_API_KEY_ID");
        String apiSecret = dotenv.get("APCA_API_SECRET_KEY");

        if (apiKey == null || apiSecret == null) {
            throw new IllegalStateException("env variables not set");
        }

        return new AlpacaTraderAPI(apiKey, apiSecret, null, TraderAPIEndpointType.PAPER, client);
    }

    @Bean
    @Profile("!backtest")
    public AccountInfoService accountInfoService(AlpacaTraderAPI alpacaTraderAPI) {
        return new AlpacaAccountService(alpacaTraderAPI);
    }

    @Bean
    @Profile("!backtest")
    public IExecutionService alpacaExecutionService(AlpacaTraderAPI alpacaTraderAPI) {
        return new AlpacaExecutionService(alpacaTraderAPI);
    }

    @Bean
    @Profile("!backtest")
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
    @Profile("!backtest")
    public Set<String> tradingSymbols(@Value("${trading.symbols}") String symbols) {
        return Set.of(symbols.split(","));
    }

    @Bean
    @Profile("!backtest")
    @ConditionalOnProperty(name = "trading.strategy.fade.enabled", havingValue = "true", matchIfMissing = true)
    public Strategy fadeTheMoveStrategy(StrategyProperties props) {
        return new FadeTheMoveStrategy(props.getFade().getWindow(), props.getFade().getZThreshold());
    }

    @Bean
    @Profile("!backtest")
    @ConditionalOnProperty(name = "trading.strategy.ppo.enabled", havingValue = "true", matchIfMissing = true)
    public Strategy ppoStrategy(RiskProperties riskProperties) {
        return new PpoStrategy(riskProperties.getConfidenceThreshold());
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
    @Profile("!backtest")
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
