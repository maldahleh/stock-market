package com.maldahleh.stockmarket.stocks.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.maldahleh.stockmarket.stocks.utils.SettingsUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

class StockProviderTests {

  private final StockProvider stockProvider = new StockProvider(SettingsUtils.buildSettings());

  @Nested
  @DisplayName("fetchStock")
  class FetchStock {

    @Test
    void success() {
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
    void cached() {
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

        yahooFinance.verify(() -> YahooFinance.get("BA", true));
      }
    }

    @Test
    void exception() {
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

  @Nested
  @DisplayName("fetchStockList")
  class FetchStockList {

    @Test
    void success() {
      // GIVEN
      String[] symbols = {"ba", "aapl", "fb"};

      try (MockedStatic<YahooFinance> yahooFinance = mockStatic(YahooFinance.class)) {
        Map<String, Stock> returnMap = new HashMap<>();
        returnMap.put("BA", new Stock("BA"));
        returnMap.put("AAPL", null);
        returnMap.put("FB", new Stock("FB"));

        yahooFinance.when(() -> YahooFinance.get(new String[]{"BA", "AAPL", "FB"}))
            .thenReturn(returnMap);

        // WHEN
        Map<String, Stock> stocks = stockProvider.get(symbols);

        // THEN
        assertEquals(2, stocks.size());
        assertFalse(stocks.containsKey("AAPL"));
      }
    }

    @Test
    void exception() {
      // GIVEN
      String[] symbols = {"ba", "aapl", "fb"};

      try (MockedStatic<YahooFinance> yahooFinance = mockStatic(YahooFinance.class)) {
        yahooFinance.when(() -> YahooFinance.get(new String[]{"BA", "AAPL", "FB"}))
            .thenThrow(new IOException());

        // WHEN
        Map<String, Stock> stocks = stockProvider.get(symbols);

        // THEN
        assertTrue(stocks.isEmpty());
      }
    }
  }
}
