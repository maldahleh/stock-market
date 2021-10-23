package com.maldahleh.stockmarket.utils;

import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.config.Settings;
import java.math.BigDecimal;
import java.util.Map;
import lombok.experimental.UtilityClass;
import yahoofinance.Stock;

@UtilityClass
public class StockDataUtils {

  public Map<String, Object> buildStockDataMap(Stock stock, BigDecimal serverPrice,
      String currencyName, Settings settings) {
    return ImmutableMap.<String, Object>builder()
        .put("<name>", stock.getName())
        .put("<exchange>", stock.getStockExchange())
        .put(
            "<cap>",
            CurrencyUtils.sigFigNumber(
                stock.getStats().getMarketCap().doubleValue()))
        .put(
            "<market-price>",
            CurrencyUtils.format(
                stock.getQuote().getPrice(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put("<market-currency>", stock.getCurrency())
        .put(
            "<server-price>",
            CurrencyUtils.format(
                serverPrice,
                settings.getUnknownData(),
                settings.getLocale()))
        .put("<server-currency>", currencyName)
        .put("<broker-flat>", settings.getBrokerFlatString())
        .put("<broker-percent>", settings.getBrokerPercentString())
        .put(
            "<change-close>",
            CurrencyUtils.format(
                stock.getQuote().getChange(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put(
            "<change-year-high>",
            CurrencyUtils.format(
                stock.getQuote().getChangeFromYearHigh(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put(
            "<change-year-low>",
            CurrencyUtils.format(
                stock.getQuote().getChangeFromYearLow(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put(
            "<change-50-moving-avg>",
            CurrencyUtils.format(
                stock.getQuote().getChangeFromAvg50(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put(
            "<change-200-moving-avg>",
            CurrencyUtils.format(
                stock.getQuote().getChangeFromAvg200(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put(
            "<yield>",
            CurrencyUtils.formatSingle(
                stock.getDividend().getAnnualYieldPercent(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put("<symbol>", stock.getSymbol().toUpperCase())
        .put(
            "<day-high>",
            CurrencyUtils.format(
                stock.getQuote().getDayHigh(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put(
            "<day-low>",
            CurrencyUtils.format(
                stock.getQuote().getDayLow(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put(
            "<open-price>",
            CurrencyUtils.format(
                stock.getQuote().getOpen(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put(
            "<volume>",
            CurrencyUtils.sigFigNumber(stock.getQuote().getVolume()))
        .put(
            "<close-price>",
            CurrencyUtils.format(
                stock.getQuote().getPreviousClose(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put(
            "<year-high>",
            CurrencyUtils.format(
                stock.getQuote().getYearHigh(),
                settings.getUnknownData(),
                settings.getLocale()))
        .put(
            "<year-low>",
            CurrencyUtils.format(
                stock.getQuote().getYearLow(),
                settings.getUnknownData(),
                settings.getLocale()))
        .build();
  }
}
