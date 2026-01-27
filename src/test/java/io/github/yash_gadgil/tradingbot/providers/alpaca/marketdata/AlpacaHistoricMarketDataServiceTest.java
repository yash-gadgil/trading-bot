package io.github.yash_gadgil.tradingbot.providers.alpaca.marketdata;

import io.github.yash_gadgil.tradingbot.core.marketdata.MarketDataException;
import io.github.yash_gadgil.tradingbot.core.model.CandleStick;
import net.jacobpeterson.alpaca.openapi.marketdata.ApiException;
import net.jacobpeterson.alpaca.openapi.marketdata.api.StockApi;
import net.jacobpeterson.alpaca.openapi.marketdata.model.StockBar;
import net.jacobpeterson.alpaca.openapi.marketdata.model.StockBarsResp;
import net.jacobpeterson.alpaca.rest.marketdata.AlpacaMarketDataAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlpacaHistoricMarketDataServiceTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2024-01-02T14:30:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2024-01-03T14:30:00Z");

    private AlpacaMarketDataAPI api;
    private StockApi stockApi;
    private AlpacaHistoricMarketDataService service;

    @BeforeEach
    void setUp() {
        api = mock(AlpacaMarketDataAPI.class);
        stockApi = mock(StockApi.class);
        when(api.stock()).thenReturn(stockApi);
        service = new AlpacaHistoricMarketDataService(api);
    }

    @Test
    void mapsBarsToCandles() throws Exception {
        StockBar bar = mock(StockBar.class);
        when(bar.getT()).thenReturn(START);
        when(bar.getV()).thenReturn(1_000L);
        when(bar.getH()).thenReturn(110.0);
        when(bar.getL()).thenReturn(90.0);
        when(bar.getO()).thenReturn(100.0);
        when(bar.getC()).thenReturn(105.0);

        StockBarsResp resp = mock(StockBarsResp.class);
        when(resp.getBars()).thenReturn(Map.of("AAPL", List.of(bar)));
        when(resp.getNextPageToken()).thenReturn(null);
        when(stockApi.stockBars(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(resp);

        List<CandleStick> candles = service.getHistoricalCandleSticks(Set.of("AAPL"), START, END);

        assertEquals(1, candles.size());
        CandleStick c = candles.getFirst();
        assertEquals("AAPL", c.symbol());
        assertEquals(105.0, c.close());
        assertEquals(110.0, c.high());
        assertEquals(1_000L, c.volume());
    }

    @Test
    void apiFailureSurfacesAsMarketDataException() throws Exception {
        when(stockApi.stockBars(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ApiException(500, "internal error"));

        assertThrows(MarketDataException.class,
                () -> service.getHistoricalCandleSticks(Set.of("AAPL"), START, END));
    }
}
