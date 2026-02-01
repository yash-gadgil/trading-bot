package io.github.yash_gadgil.tradingbot.providers.alpaca.account;

import io.github.yash_gadgil.tradingbot.core.account.AccountInfoService;
import io.github.yash_gadgil.tradingbot.core.account.AccountSnapshot;
import io.github.yash_gadgil.tradingbot.core.account.HeldPosition;
import net.jacobpeterson.alpaca.openapi.trader.ApiException;
import net.jacobpeterson.alpaca.openapi.trader.model.Account;
import net.jacobpeterson.alpaca.openapi.trader.model.Position;
import net.jacobpeterson.alpaca.rest.trader.AlpacaTraderAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AlpacaAccountService implements AccountInfoService {

    private static final Logger logger = LoggerFactory.getLogger(AlpacaAccountService.class);

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

    @Override
    public List<HeldPosition> getOpenPositions() {
        try {
            List<Position> positions = alpacaTraderAPI.positions().getAllOpenPositions();
            List<HeldPosition> out = new ArrayList<>(positions.size());
            for (Position p : positions) {
                int qty = (int) Math.round(Double.parseDouble(p.getQty()));

                if (p.getSide() != null && p.getSide().toLowerCase().contains("short")) {
                    qty = -Math.abs(qty);
                }
                out.add(new HeldPosition(p.getSymbol(), qty, Double.parseDouble(p.getAvgEntryPrice())));
            }
            return out;
        } catch (ApiException | RuntimeException e) {
            logger.warn("Failed to fetch open positions for reconciliation: {}", e.getMessage());
            return List.of();
        }
    }

}
