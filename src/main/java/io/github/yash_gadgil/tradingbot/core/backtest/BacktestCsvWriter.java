package io.github.yash_gadgil.tradingbot.core.backtest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BacktestCsvWriter {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("America/New_York"));

    public static void write(BacktestResult result, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        writeEquity(result, outDir.resolve("equity.csv"));
        writeTrades(result, outDir.resolve("trades.csv"));
    }

    private static void writeEquity(BacktestResult result, Path file) throws IOException {
        List<EquityPoint> equity = result.equityCurve();
        List<EquityPoint> bench = result.benchmarkCurve();
        StringBuilder sb = new StringBuilder("timestamp,equity,benchmark\n");
        for (int i = 0; i < equity.size(); i++) {
            EquityPoint p = equity.get(i);
            double b = i < bench.size() ? bench.get(i).equity() : 0.0;
            sb.append(FMT.format(p.timestamp())).append(',')
                    .append(String.format("%.2f", p.equity())).append(',')
                    .append(String.format("%.2f", b)).append('\n');
        }
        Files.writeString(file, sb.toString());
    }

    private static void writeTrades(BacktestResult result, Path file) throws IOException {
        StringBuilder sb = new StringBuilder("entry_time,exit_time,symbol,side,entry_price,exit_price,quantity,pnl\n");
        for (SimulatedTrade t : result.trades()) {
            sb.append(FMT.format(t.entryTime())).append(',')
                    .append(FMT.format(t.exitTime())).append(',')
                    .append(t.symbol()).append(',')
                    .append(t.side()).append(',')
                    .append(String.format("%.2f", t.entryPrice())).append(',')
                    .append(String.format("%.2f", t.exitPrice())).append(',')
                    .append(t.quantity()).append(',')
                    .append(String.format("%.2f", t.pnl())).append('\n');
        }
        Files.writeString(file, sb.toString());
    }
}
