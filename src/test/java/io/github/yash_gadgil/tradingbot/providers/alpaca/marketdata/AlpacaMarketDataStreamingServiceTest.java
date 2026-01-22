package io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata;

import net.jacobpeterson.alpaca.websocket.marketdata.MarketDataWebsocket;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AlpacaMarketDataStreamingServiceTest {

    @Mock
    private MarketDataWebsocket marketDataWebsocket;

    @InjectMocks
    private AlpacaMarketDataStreamingService marketDataStreamingService;

}
