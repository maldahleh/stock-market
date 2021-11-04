package com.maldahleh.stockmarket.stocks.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import com.maldahleh.stockmarket.stocks.utils.SettingsUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.fx.FxQuote;

class ForexProviderTests {

  private final ForexProvider forexProvider = new ForexProvider(SettingsUtils.buildSettings());

  @Nested
  @DisplayName("fetchForex")
  class FetchForex {

    @Test
    void success() {
      // GIVEN
      String targetCurrency = "cad";

      try (MockedStatic<YahooFinance> yahooFinance = mockStatic(YahooFinance.class)) {
        yahooFinance.when(() -> YahooFinance.getFx("CADUSD=X"))
            .thenReturn(new FxQuote("CADUSD=X", BigDecimal.ONE));

        // WHEN
        BigDecimal price = forexProvider.getFxRate(targetCurrency);

        // THEN
        assertNotNull(price);
        assertEquals(BigDecimal.ONE, price);
      }
    }

    @Test
    void exception() {
      // GIVEN
      String targetCurrency = "cad";

      try (MockedStatic<YahooFinance> yahooFinance = mockStatic(YahooFinance.class)) {
        yahooFinance.when(() -> YahooFinance.getFx("CADUSD=X"))
            .thenThrow(new IOException());

        // WHEN
        BigDecimal price = forexProvider.getFxRate(targetCurrency);

        // THEN
        assertNull(price);
      }
    }
  }

  @Nested
  @DisplayName("fetchForexList")
  class FetchForexList {

    @Test
    void success() {
      // GIVEN
      String[] targetCurrency = {"cad", "eur"};

      try (MockedStatic<YahooFinance> yahooFinance = mockStatic(YahooFinance.class)) {
        yahooFinance.when(() -> YahooFinance.getFx("CAD"))
            .thenReturn(new FxQuote("CAD", BigDecimal.ONE));

        yahooFinance.when(() -> YahooFinance.getFx("EUR"))
            .thenReturn(new FxQuote("EUR", BigDecimal.TEN));

        // WHEN
        Map<String, FxQuote> quotes = forexProvider.get(targetCurrency);

        // THEN
        assertEquals(2, quotes.size());
        assertEquals(BigDecimal.ONE, quotes.get("CAD").getPrice());
        assertEquals(BigDecimal.TEN, quotes.get("EUR").getPrice());
      }
    }
  }
}
