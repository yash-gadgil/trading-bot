package io.github.yash_gadgil.tradingbot.providers.alpaca.account;

import io.github.yash_gadgil.tradingbot.core.account.AccountInfoService;
import io.github.yash_gadgil.tradingbot.core.account.AccountSnapshot;
import net.jacobpeterson.alpaca.openapi.trader.ApiException;
import net.jacobpeterson.alpaca.openapi.trader.model.Account;
import net.jacobpeterson.alpaca.rest.trader.AlpacaTraderAPI;

import java.util.Objects;

public class AlpacaAccountService implements AccountInfoService {

    private final AlpacaTraderAPI alpacaTraderAPI;

    public AlpacaAccountService(AlpacaTraderAPI alpacaTraderAPI) {
        this.alpacaTraderAPI = alpacaTraderAPI;
    }

    @Override
    public AccountSnapshot getAccountSnapshot() {
        try {
            Account alpacaAccount = alpacaTraderAPI.accounts().getAccount();

            return new AccountSnapshot(
                alpacaAccount.getCurrency(),
                Double.parseDouble(Objects.requireNonNull(alpacaAccount.getBuyingPower())),
                Integer.parseInt(Objects.requireNonNull(alpacaAccount.getMultiplier()))
            );

        } catch (ApiException e) {

            return null;

        }
    }

}
