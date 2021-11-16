package com.maldahleh.stockmarket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.config.models.BrokerSettings;
import com.maldahleh.stockmarket.config.models.SqlSettings;
import java.io.File;
import java.math.BigDecimal;
import java.util.Locale;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SettingsTests {

  private Settings settings;

  @BeforeEach
  void setup() {
    JavaPlugin javaPlugin = mock(JavaPlugin.class);

    when(javaPlugin.getDataFolder())
        .thenReturn(new File("src/test/resources"));

    this.settings = new Settings(javaPlugin);
  }

  @Test
  void brokerSettings() {
    // WHEN
    BrokerSettings brokerSettings = settings.getBrokerSettings();

    // THEN
    assertNotNull(brokerSettings);
  }

  @Test
  void sqlSettings() {
    // WHEN
    SqlSettings sqlSettings = settings.getSqlSettings();

    // THEN
    assertNotNull(sqlSettings);
  }

  @Test
  void locale() {
    // WHEN
    Locale locale = settings.getLocale();

    // THEN
    assertEquals(Locale.US, locale);
  }

  @Test
  void cacheMinutes() {
    // WHEN
    int cacheMinutes = settings.getCacheMinutes();

    // THEN
    assertEquals(5, cacheMinutes);
  }

  @Test
  void unknownData() {
    // WHEN
    String unknownData = settings.getUnknownData();

    // THEN
    assertEquals("N/A", unknownData);
  }

  @Test
  void transactionCooldownSeconds() {
    // WHEN
    int cooldownSeconds = settings.getTransactionCooldownSeconds();

    // THEN
    assertEquals(2, cooldownSeconds);
  }

  @Test
  void minutesBetweenSale() {
    // WHEN
    int minutesBetweenSale = settings.getMinutesBetweenSale();

    // THEN
    assertEquals(5, minutesBetweenSale);
  }

  @Test
  void priceMultiplier() {
    // WHEN
    BigDecimal multiplier = settings.getPriceMultiplier();

    // THEN
    assertEquals(BigDecimal.valueOf(20.0), multiplier);
  }

  @ParameterizedTest
  @ValueSource(doubles = {5.0, 9.0})
  void aboveMinimumPrice(double price) {
    // GIVEN
    BigDecimal stockPrice = BigDecimal.valueOf(price);

    // WHEN
    boolean isAboveMinimum = settings.isAboveMinimumPrice(stockPrice);

    // THEN
    assertTrue(isAboveMinimum);
  }

  @Test
  void belowMinimumPrice() {
    // GIVEN
    BigDecimal stockPrice = BigDecimal.ONE;

    // WHEN
    boolean isAboveMinimum = settings.isAboveMinimumPrice(stockPrice);

    // THEN
    assertFalse(isAboveMinimum);
  }

  @Test
  void allowedCurrency() {
    // GIVEN
    String currency = "USD";

    // WHEN
    boolean isAllowedCurrency = settings.isAllowedCurrency(currency);

    // THEN
    assertTrue(isAllowedCurrency);
  }

  @Test
  void notAllowedCurrency() {
    // GIVEN
    String currency = "EUR";

    // WHEN
    boolean isAllowedCurrency = settings.isAllowedCurrency(currency);

    // THEN
    assertFalse(isAllowedCurrency);
  }

  @Test
  void allowedExchange() {
    // GIVEN
    String exchange = "NYSE";

    // WHEN
    boolean isAllowedExchange = settings.isAllowedExchange(exchange);

    // THEN
    assertTrue(isAllowedExchange);
  }

  @Nested
  class Format {

    @Test
    void nullValue() {
      // WHEN
      String formatted = settings.format(null);

      // THEN
      assertEquals("N/A", formatted);
    }

    @Test
    void validValue() {
      // GIVEN
      BigDecimal value = BigDecimal.valueOf(1_000_000);

      // WHEN
      String formatted = settings.format(value);

      // THEN
      assertEquals("1,000,000.00", formatted);
    }
  }

  @Nested
  class FormatSingle {

    @Test
    void nullValue() {
      // WHEN
      String formatted = settings.formatSingle(null);

      // THEN
      assertEquals("N/A", formatted);
    }

    @Test
    void validValue() {
      // GIVEN
      BigDecimal value = BigDecimal.valueOf(12.57);

      // WHEN
      String formatted = settings.formatSingle(value);

      // THEN
      assertEquals("12.6", formatted);
    }
  }
}
