package com.maldahleh.stockmarket.stocks.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.maldahleh.stockmarket.stocks.utils.SettingsUtils;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

class StockProviderTests {

  private final StockProvider stockProvider = new StockProvider(SettingsUtils.buildSettings());

  @Test
  void fetchStock() {
    // GIVEN
    String symbol = "ba";

    try (MockedStatic<YahooFinance> yahooFinance = mockStatic(YahooFinance.class)) {
      yahooFinance.when(() -> YahooFinance.get("BA", true))
          .thenReturn(new Stock("BA"));

      // WHEN
      Stock stock = stockProvider.get(symbol);

      // THEN
      assertNotNull(stock);
    }
  }

  @Test
  void fetchStockCached() {
    // GIVEN
    String symbol = "ba";

    try (MockedStatic<YahooFinance> yahooFinance = mockStatic(YahooFinance.class)) {
      yahooFinance.when(() -> YahooFinance.get("BA", true))
          .thenReturn(new Stock("BA"));

      // WHEN
      Stock stock = stockProvider.get(symbol);
      Stock stockTwo = stockProvider.get(symbol);

      // THEN
      assertEquals(stock, stockTwo);

      yahooFinance.verify(() -> YahooFinance.get("BA", true), times(1));
    }
  }

  @Test
  void fetchStockException() {
    // GIVEN
    String symbol = "ba";

    try (MockedStatic<YahooFinance> yahooFinance = mockStatic(YahooFinance.class)) {
      yahooFinance.when(() -> YahooFinance.get("BA", true))
          .thenThrow(new IOException());

      // WHEN
      Stock stock = stockProvider.get(symbol);

      // THEN
      assertNull(stock);
    }
  }
}
