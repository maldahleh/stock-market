package com.maldahleh.stockmarket.stocks.provider;

import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.stocks.common.CacheableProvider;
import java.io.IOException;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

public class StockProvider extends CacheableProvider<Stock> {

  public StockProvider(Settings settings) {
    super(settings);
  }

  public Stock getStock(String symbol) {
    return get(symbol);
  }

  @Override
  protected Stock fetch(String key) {
    try {
      return YahooFinance.get(key, true);
    } catch (IOException e) {
      return null;
    }
  }
}
