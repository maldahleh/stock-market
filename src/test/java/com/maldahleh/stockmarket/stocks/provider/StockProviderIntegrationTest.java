package com.maldahleh.stockmarket.stocks.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.maldahleh.stockmarket.stocks.utils.SettingsUtils;
import org.junit.jupiter.api.Test;
import yahoofinance.Stock;

class StockProviderIntegrationTest {

  private final StockProvider stockProvider = new StockProvider(SettingsUtils.buildSettings());

  @Test
  void fetchStock() {
    // GIVEN
    String symbol = "BA";

    // WHEN
    Stock stock = stockProvider.get(symbol);

    // THEN
    assertNotNull(stock);
  }
}
