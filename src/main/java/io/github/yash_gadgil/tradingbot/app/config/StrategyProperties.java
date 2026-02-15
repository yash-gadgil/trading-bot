package io.github.yash_gadgil.tradingbot.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trading.strategy")
public class StrategyProperties {

    private Fade fade = new Fade();
    private Ppo ppo = new Ppo();

    public Fade getFade() { return fade; }
    public void setFade(Fade fade) { this.fade = fade; }

    public Ppo getPpo() { return ppo; }
    public void setPpo(Ppo ppo) { this.ppo = ppo; }

    public static class Fade {
        private boolean enabled = true;
        private int window = 14;
        private double zThreshold = 2.0;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getWindow() { return window; }
        public void setWindow(int window) { this.window = window; }
        public double getZThreshold() { return zThreshold; }
        public void setZThreshold(double zThreshold) { this.zThreshold = zThreshold; }
    }

    public static class Ppo {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
