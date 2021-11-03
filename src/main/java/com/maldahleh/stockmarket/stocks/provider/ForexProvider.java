package com.maldahleh.stockmarket.stocks.provider;

import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.stocks.common.CacheableProvider;
import java.io.IOException;
import java.math.BigDecimal;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.fx.FxQuote;

public class ForexProvider extends CacheableProvider<FxQuote> {

  public static final String USD = "USD";

  public ForexProvider(Settings settings) {
    super(settings);
  }

  public BigDecimal getFxRate(String targetCurrency) {
    String fxQuote = targetCurrency + USD + "=X";

    return get(fxQuote).getPrice();
  }

  @Override
  protected FxQuote fetch(String key) {
    try {
      return YahooFinance.getFx(key.toUpperCase());
    } catch (IOException e) {
      return null;
    }
  }
}
