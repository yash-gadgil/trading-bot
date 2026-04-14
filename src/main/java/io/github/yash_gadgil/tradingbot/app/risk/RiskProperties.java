package io.github.yash_gadgil.tradingbot.app.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trading.risk")
public class RiskProperties {

    private double perOrderFraction = 0.01;

    private int maxPositionPerSymbol = 100;

    private double maxGrossExposure = 0.5;

    private double dailyLossLimit = 1000.0;

    private double confidenceThreshold = 0.55;

    private boolean flattenOnKill = false;

    public double getPerOrderFraction() { return perOrderFraction; }
    public void setPerOrderFraction(double v) { this.perOrderFraction = v; }

    public int getMaxPositionPerSymbol() { return maxPositionPerSymbol; }
    public void setMaxPositionPerSymbol(int v) { this.maxPositionPerSymbol = v; }

    public double getMaxGrossExposure() { return maxGrossExposure; }
    public void setMaxGrossExposure(double v) { this.maxGrossExposure = v; }

    public double getDailyLossLimit() { return dailyLossLimit; }
    public void setDailyLossLimit(double v) { this.dailyLossLimit = v; }

    public double getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(double v) { this.confidenceThreshold = v; }

    public boolean isFlattenOnKill() { return flattenOnKill; }
    public void setFlattenOnKill(boolean v) { this.flattenOnKill = v; }
}
