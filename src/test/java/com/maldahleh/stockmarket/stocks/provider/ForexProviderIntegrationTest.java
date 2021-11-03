package com.maldahleh.stockmarket.stocks.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.maldahleh.stockmarket.stocks.utils.SettingsBuilder;
import java.math.BigDecimal;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("This test is blocking, and should be run outside pipeline")
class ForexProviderIntegrationTest {

  private final ForexProvider forexProvider = new ForexProvider(SettingsBuilder.buildSettings());

  @Test
  void fetchForex() {
    // GIVEN
    String targetCurrency = "CAD";

    // WHEN
    BigDecimal rate = forexProvider.getFxRate(targetCurrency);

    // THEN
    System.out.println("Rate: " + rate);
    assertNotNull(rate);
  }
}