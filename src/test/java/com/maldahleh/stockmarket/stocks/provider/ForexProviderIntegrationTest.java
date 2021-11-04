package com.maldahleh.stockmarket.stocks.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.maldahleh.stockmarket.stocks.utils.SettingsUtils;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ForexProviderIntegrationTest {

  private final ForexProvider forexProvider = new ForexProvider(SettingsUtils.buildSettings());

  @Test
  void fetchForex() {
    // GIVEN
    String targetCurrency = "CAD";

    // WHEN
    BigDecimal rate = forexProvider.getFxRate(targetCurrency);

    // THEN
    assertNotNull(rate);
  }
}
