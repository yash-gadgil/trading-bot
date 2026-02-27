package io.github.yash_gadgil.tradingbot.providers.onnx;

import io.github.yash_gadgil.tradingbot.core.model.CandleStick;

import java.util.Collection;

final class PpoFeatureEngineer {

    static final int OBSERVATION_SIZE = 90;
    static final int MIN_HISTORY = 110;

    private static final int WINDOW_SIZE = 10;
    private static final int Z_WINDOW = 100;
    private static final int NUM_FEATURES = 8;
    private static final double EPS = 1e-8;

    private PpoFeatureEngineer() {}

    static float[] buildObservation(Collection<CandleStick> history, int logicalPosition) {
        if (history.size() < MIN_HISTORY) {
            throw new IllegalArgumentException(
                    "Need at least " + MIN_HISTORY + " candles for PPO feature engineering, got " + history.size());
        }

        int n = history.size();
        double[] open = new double[n];
        double[] high = new double[n];
        double[] low = new double[n];
        double[] close = new double[n];
        double[] volume = new double[n];
        int i = 0;
        for (CandleStick c : history) {
            open[i] = c.open();
            high[i] = c.high();
            low[i] = c.low();
            close[i] = c.close();
            volume[i] = c.volume();
            i++;
        }

        double[] ret = pctChange(close);

        double[] sma10 = rollingMean(close, 10);
        double[] sma30 = rollingMean(close, 30);
        double[] sma10Ratio = new double[n];
        double[] sma30Ratio = new double[n];
        for (int k = 0; k < n; k++) {
            sma10Ratio[k] = Double.isNaN(sma10[k]) ? Double.NaN : close[k] / sma10[k] - 1.0;
            sma30Ratio[k] = Double.isNaN(sma30[k]) ? Double.NaN : close[k] / sma30[k] - 1.0;
        }

        double[] delta = diff(close);
        double[] up = new double[n];
        double[] down = new double[n];
        for (int k = 0; k < n; k++) {
            if (Double.isNaN(delta[k])) {
                up[k] = Double.NaN;
                down[k] = Double.NaN;
            } else {
                up[k] = Math.max(delta[k], 0.0);
                down[k] = -Math.min(delta[k], 0.0);
            }
        }
        double[] emaUp = ewmAlphaAdjustFalse(up, 1.0 / 14.0);
        double[] emaDown = ewmAlphaAdjustFalse(down, 1.0 / 14.0);
        double[] rsi = new double[n];
        for (int k = 0; k < n; k++) {
            if (Double.isNaN(emaUp[k]) || Double.isNaN(emaDown[k])) {
                rsi[k] = Double.NaN;
            } else {
                double rs = emaUp[k] / (emaDown[k] + EPS);
                rsi[k] = (100.0 - (100.0 / (1.0 + rs))) / 100.0;
            }
        }

        double[] ema12 = ewmAlphaAdjustFalse(close, 2.0 / 13.0);
        double[] ema26 = ewmAlphaAdjustFalse(close, 2.0 / 27.0);
        double[] macd = new double[n];
        for (int k = 0; k < n; k++) {
            macd[k] = ema12[k] - ema26[k];
        }
        double[] macdSignal = ewmAlphaAdjustFalse(macd, 2.0 / 10.0);
        double[] macdHist = new double[n];
        for (int k = 0; k < n; k++) {
            macdHist[k] = (macd[k] - macdSignal[k]) / close[k];
        }

        double[] sma20 = rollingMean(close, 20);
        double[] std20 = rollingStd(close, 20);
        double[] bbPos = new double[n];
        for (int k = 0; k < n; k++) {
            if (Double.isNaN(sma20[k]) || Double.isNaN(std20[k])) {
                bbPos[k] = Double.NaN;
            } else {
                double upper = sma20[k] + 2.0 * std20[k];
                double lower = sma20[k] - 2.0 * std20[k];
                bbPos[k] = (close[k] - lower) / (upper - lower + EPS);
            }
        }

        double[] tr = new double[n];
        tr[0] = high[0] - low[0];
        for (int k = 1; k < n; k++) {
            double a = high[k] - low[k];
            double b = Math.abs(high[k] - close[k - 1]);
            double c = Math.abs(low[k] - close[k - 1]);
            tr[k] = Math.max(a, Math.max(b, c));
        }
        double[] atrMean = rollingMean(tr, 14);
        double[] atr = new double[n];
        for (int k = 0; k < n; k++) {
            atr[k] = Double.isNaN(atrMean[k]) ? Double.NaN : atrMean[k] / close[k];
        }

        double[] vol = volume.clone();

        double[][] cols = {ret, sma10Ratio, sma30Ratio, rsi, macdHist, bbPos, atr, vol};

        for (double[] col : cols) {
            backfillInPlace(col);
            fillNaInPlace(col, 0.0);
        }

        for (int f = 0; f < cols.length; f++) {
            cols[f] = rollingZScore(cols[f], Z_WINDOW);
            backfillInPlace(cols[f]);
            fillNaInPlace(cols[f], 0.0);
        }

        float[] obs = new float[WINDOW_SIZE * 9];
        int start = n - WINDOW_SIZE;
        float pos = (float) logicalPosition;
        for (int r = 0; r < WINDOW_SIZE; r++) {
            int row = start + r;
            for (int f = 0; f < NUM_FEATURES; f++) {
                obs[r * 9 + f] = (float) cols[f][row];
            }
            obs[r * 9 + NUM_FEATURES] = pos;
        }
        return obs;
    }

    private static double[] pctChange(double[] x) {
        double[] out = new double[x.length];
        out[0] = Double.NaN;
        for (int k = 1; k < x.length; k++) {
            out[k] = (x[k] - x[k - 1]) / x[k - 1];
        }
        return out;
    }

    private static double[] diff(double[] x) {
        double[] out = new double[x.length];
        out[0] = Double.NaN;
        for (int k = 1; k < x.length; k++) {
            out[k] = x[k] - x[k - 1];
        }
        return out;
    }

    private static double[] rollingMean(double[] x, int w) {
        double[] out = new double[x.length];
        for (int k = 0; k < x.length; k++) {
            if (k < w - 1) {
                out[k] = Double.NaN;
                continue;
            }
            double sum = 0.0;
            for (int j = k - w + 1; j <= k; j++) {
                sum += x[j];
            }
            out[k] = sum / w;
        }
        return out;
    }

    private static double[] rollingStd(double[] x, int w) {
        double[] out = new double[x.length];
        for (int k = 0; k < x.length; k++) {
            if (k < w - 1) {
                out[k] = Double.NaN;
                continue;
            }
            double sum = 0.0;
            for (int j = k - w + 1; j <= k; j++) {
                sum += x[j];
            }
            double mean = sum / w;
            double sumSq = 0.0;
            for (int j = k - w + 1; j <= k; j++) {
                double d = x[j] - mean;
                sumSq += d * d;
            }
            out[k] = Math.sqrt(sumSq / (w - 1));
        }
        return out;
    }

    private static double[] ewmAlphaAdjustFalse(double[] x, double alpha) {
        double[] out = new double[x.length];
        double prev = Double.NaN;
        for (int k = 0; k < x.length; k++) {
            double v = x[k];
            if (Double.isNaN(prev)) {
                out[k] = v;
                if (!Double.isNaN(v)) {
                    prev = v;
                }
            } else if (Double.isNaN(v)) {
                out[k] = prev;
            } else {
                prev = alpha * v + (1.0 - alpha) * prev;
                out[k] = prev;
            }
        }
        return out;
    }

    private static double[] rollingZScore(double[] x, int w) {
        double[] out = new double[x.length];
        for (int k = 0; k < x.length; k++) {
            int start = Math.max(0, k - w + 1);
            int count = k - start + 1;
            double sum = 0.0;
            for (int j = start; j <= k; j++) {
                sum += x[j];
            }
            double mean = sum / count;
            if (count < 2) {
                out[k] = Double.NaN;
                continue;
            }
            double sumSq = 0.0;
            for (int j = start; j <= k; j++) {
                double d = x[j] - mean;
                sumSq += d * d;
            }
            double std = Math.sqrt(sumSq / (count - 1));
            out[k] = (x[k] - mean) / (std + EPS);
        }
        return out;
    }

    private static void backfillInPlace(double[] x) {

        double next = Double.NaN;
        for (int k = x.length - 1; k >= 0; k--) {
            if (Double.isNaN(x[k])) {
                if (!Double.isNaN(next)) {
                    x[k] = next;
                }
            } else {
                next = x[k];
            }
        }
    }

    private static void fillNaInPlace(double[] x, double fill) {
        for (int k = 0; k < x.length; k++) {
            if (Double.isNaN(x[k])) {
                x[k] = fill;
            }
        }
    }
}
