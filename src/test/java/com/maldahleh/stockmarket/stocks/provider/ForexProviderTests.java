package com.maldahleh.stockmarket.stocks.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import com.maldahleh.stockmarket.stocks.utils.SettingsUtils;
import java.io.IOException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.fx.FxQuote;

class ForexProviderTests {

  private final ForexProvider forexProvider = new ForexProvider(SettingsUtils.buildSettings());

  @Test
  void fetchForex() {
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
  void fetchForexException() {
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
