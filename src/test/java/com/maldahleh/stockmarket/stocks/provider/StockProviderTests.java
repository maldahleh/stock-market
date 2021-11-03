package com.maldahleh.stockmarket.stocks.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.maldahleh.stockmarket.stocks.utils.SettingsBuilder;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

class StockProviderTests {

  private final StockProvider stockProvider = new StockProvider(SettingsBuilder.buildSettings());

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
      stockProvider.get(symbol);
      stockProvider.get(symbol);

      // THEN
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
