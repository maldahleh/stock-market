package com.maldahleh.stockmarket.stocks.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import org.bukkit.configuration.ConfigurationSection;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.fx.FxQuote;

@UtilityClass
public class StockUtils {

  public Stock fetchStock(String symbol) {
    try {
      return YahooFinance.get(symbol, true);
    } catch (IOException e) {
      return null;
    }
  }

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

  public FxQuote fetchFxQuote(String fxQuote) {
    try {
      return YahooFinance.getFx(fxQuote);
    } catch (IOException e) {
      return null;
    }
  }

  public <T> Cache<String, T> buildCache(ConfigurationSection section) {
    return buildCache(section.getInt("cache.expire-minutes"));
  }

  public <T> Cache<String, T> buildCache(int minutes) {
    return CacheBuilder.newBuilder()
        .expireAfterWrite(minutes, TimeUnit.MINUTES)
        .maximumSize(500)
        .build();
  }
}
