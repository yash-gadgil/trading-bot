package io.github.yash_gadgil.tradingbot.core.account;

import java.util.List;

public interface AccountInfoService {

    AccountSnapshot getAccountSnapshot();

    default List<HeldPosition> getOpenPositions() {
        return List.of();
    }
}
