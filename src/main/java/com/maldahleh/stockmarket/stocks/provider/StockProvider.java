package com.maldahleh.stockmarket.stocks.provider;

import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.stocks.common.CacheableProvider;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

public class StockProvider extends CacheableProvider<Stock> {

  public StockProvider(Settings settings) {
    super(settings);
  }

  @Override
  protected Stock fetch(String key) {
    try {
      return YahooFinance.get(key, true);
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  protected Map<String, Stock> fetch(String[] keys) {
    try {
      return YahooFinance.get(keys);
    } catch (IOException e) {
      return new HashMap<>();
    }
  }
}
