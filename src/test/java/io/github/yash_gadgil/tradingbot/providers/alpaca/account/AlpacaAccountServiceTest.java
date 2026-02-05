package io.github.yash_gadgil.tradingbot.providers.alpaca.account;

import io.github.yash_gadgil.tradingbot.core.account.AccountSnapshot;
import net.jacobpeterson.alpaca.openapi.trader.api.AccountsApi;
import net.jacobpeterson.alpaca.openapi.trader.model.Account;
import net.jacobpeterson.alpaca.rest.trader.AlpacaTraderAPI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlpacaAccountServiceTest {

    @Mock
    private AlpacaTraderAPI alpacaTraderAPI;

    @InjectMocks
    private AlpacaAccountService alpacaAccountService;

    @Test
    void accountInfoAPITest() throws Exception {

        Account account = new Account();
        account.setCurrency("USD");
        account.setBuyingPower("1500.50");
        account.setMultiplier("2");

        AccountsApi accountsApiMock = mock(AccountsApi.class);
        when(alpacaTraderAPI.accounts()).thenReturn(accountsApiMock);
        when(accountsApiMock.getAccount()).thenReturn(account);

        AccountSnapshot snapshot = alpacaAccountService.getAccountSnapshot();

        assertNotNull(snapshot);
        assertEquals("USD", snapshot.currency());
        assertEquals(1500.50, snapshot.totalBalance());
        assertEquals(2, snapshot.multiplier());

    }
}
