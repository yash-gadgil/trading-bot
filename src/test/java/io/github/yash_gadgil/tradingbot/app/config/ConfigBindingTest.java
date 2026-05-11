package io.github.yash_gadgil.tradingbot.app.config;

import io.github.yash_gadgil.tradingbot.app.risk.RiskProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigBindingTest {

    @Test
    void riskPropertiesBind() {
        var source = new MapConfigurationPropertySource(Map.of(
                "trading.risk.per-order-fraction", "0.02",
                "trading.risk.max-position-per-symbol", "50",
                "trading.risk.max-gross-exposure", "0.75",
                "trading.risk.daily-loss-limit", "2500",
                "trading.risk.confidence-threshold", "0.6",
                "trading.risk.flatten-on-kill", "true"));

        RiskProperties p = new Binder(source).bind("trading.risk", RiskProperties.class).get();
        assertEquals(0.02, p.getPerOrderFraction());
        assertEquals(50, p.getMaxPositionPerSymbol());
        assertEquals(0.75, p.getMaxGrossExposure());
        assertEquals(2500, p.getDailyLossLimit());
        assertEquals(0.6, p.getConfidenceThreshold());
        assertTrue(p.isFlattenOnKill());
    }

    @Test
    void strategyPropertiesBindWithRelaxedNames() {
        var source = new MapConfigurationPropertySource(Map.of(
                "trading.strategy.fade.enabled", "false",
                "trading.strategy.fade.window", "20",
                "trading.strategy.fade.z-threshold", "2.5",
                "trading.strategy.ppo.enabled", "true"));

        StrategyProperties p = new Binder(source).bind("trading.strategy", StrategyProperties.class).get();
        assertFalse(p.getFade().isEnabled());
        assertEquals(20, p.getFade().getWindow());
        assertEquals(2.5, p.getFade().getZThreshold());
        assertTrue(p.getPpo().isEnabled());
    }

    @Test
    void defaultsApplyWhenUnset() {
        var source = new MapConfigurationPropertySource(Map.of());
        RiskProperties p = new Binder(source).bind("trading.risk", RiskProperties.class)
                .orElseGet(RiskProperties::new);
        assertEquals(0.01, p.getPerOrderFraction());
        assertEquals(100, p.getMaxPositionPerSymbol());
        assertEquals(0.5, p.getMaxGrossExposure());
    }
}
