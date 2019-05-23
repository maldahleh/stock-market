package com.maldahleh.stockmarket.stocks;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.bukkit.configuration.ConfigurationSection;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.fx.FxQuote;

public class StockManager {
  private final Cache<String, Stock> stockCache;
  private final Cache<String, FxQuote> fxCache;

  public StockManager(ConfigurationSection section) {
    this.stockCache = CacheBuilder.newBuilder()
        .expireAfterWrite(section.getInt("cache.expire-minutes"), TimeUnit.MINUTES)
        .maximumSize(500).build();
    this.fxCache = CacheBuilder.newBuilder()
        .expireAfterWrite(section.getInt("cache.expire-minutes"), TimeUnit.MINUTES)
        .maximumSize(500).build();
  }

  public Stock getStock(String symbol) {
    String upperSymbol = symbol.toUpperCase();
    Stock stock = stockCache.getIfPresent(upperSymbol);
    if (stock != null) {
      return stock;
    }

    try {
      stock = YahooFinance.get(upperSymbol, true);
      stockCache.put(upperSymbol, stock);
      return stock;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public BigDecimal getFxRate(String fxSymbol) {
    String fxQuote = fxSymbol.toUpperCase() + "USD=X";
    FxQuote quote = fxCache.getIfPresent(fxQuote);
    if (quote != null) {
      return quote.getPrice();
    }

    try {
      quote = YahooFinance.getFx(fxQuote);
      fxCache.put(fxQuote, quote);
      return quote.getPrice();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
