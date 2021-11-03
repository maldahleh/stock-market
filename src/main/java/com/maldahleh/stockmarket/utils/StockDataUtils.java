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
        .put("<cap>", CurrencyUtils.sigFigNumber(stock.getStats().getMarketCap()))
        .put("<market-price>", CurrencyUtils.format(stock.getQuote().getPrice(), settings))
        .put("<market-currency>", stock.getCurrency())
        .put("<server-price>", CurrencyUtils.format(serverPrice, settings))
        .put("<server-currency>", currencyName)
        .put("<broker-flat>", settings.getBrokerSettings().getBrokerFlatString())
        .put("<broker-percent>", settings.getBrokerSettings().getBrokerPercentString())
        .put("<change-close>", CurrencyUtils.format(stock.getQuote().getChange(), settings))
        .put("<change-year-high>",
            CurrencyUtils.format(stock.getQuote().getChangeFromYearHigh(), settings))
        .put("<change-year-low>",
            CurrencyUtils.format(stock.getQuote().getChangeFromYearLow(), settings))
        .put("<change-50-moving-avg>",
            CurrencyUtils.format(stock.getQuote().getChangeFromAvg50(), settings))
        .put("<change-200-moving-avg>",
            CurrencyUtils.format(stock.getQuote().getChangeFromAvg200(), settings))
        .put("<yield>",
            CurrencyUtils.formatSingle(stock.getDividend().getAnnualYieldPercent(), settings))
        .put("<symbol>", stock.getSymbol().toUpperCase())
        .put("<day-high>", CurrencyUtils.format(stock.getQuote().getDayHigh(), settings))
        .put("<day-low>", CurrencyUtils.format(stock.getQuote().getDayLow(), settings))
        .put("<open-price>", CurrencyUtils.format(stock.getQuote().getOpen(), settings))
        .put("<volume>", CurrencyUtils.sigFigNumber(stock.getQuote().getVolume()))
        .put("<close-price>", CurrencyUtils.format(stock.getQuote().getPreviousClose(), settings))
        .put("<year-high>", CurrencyUtils.format(stock.getQuote().getYearHigh(), settings))
        .put("<year-low>", CurrencyUtils.format(stock.getQuote().getYearLow(), settings))
        .build();
  }
}
