package com.maldahleh.stockmarket.stocks.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

@UtilityClass
public class StockUtils {

  public Map<String, Stock> fetchStocks(String... symbols) {
    Map<String, Stock> stocks = lookupStocks(symbols);
    stocks.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);

    return stocks;
  }

  private Map<String, Stock> lookupStocks(String... symbols) {
    try {
      return YahooFinance.get(symbols);
    } catch (IOException e) {
      return new HashMap<>();
    }
  }
}
