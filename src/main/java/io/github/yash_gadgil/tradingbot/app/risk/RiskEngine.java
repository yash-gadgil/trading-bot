package io.github.yash_gadgil.tradingbot.app.risk;

import io.github.yash_gadgil.tradingbot.core.account.AccountInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

@Service
public class RiskEngine implements SmartLifecycle {

    private final AccountInfoService accountInfoService;

    @Autowired
    public RiskEngine(AccountInfoService accountInfoService) {
        this.accountInfoService = accountInfoService;
    }


    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
