package com.maldahleh.stockmarket.config.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.config.common.ConfigSection;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BrokerSettingsTests {

  private ConfigSection configSection;
  private BrokerSettings brokerSettings;

  @BeforeEach
  void setup() {
    this.configSection = mock(ConfigSection.class);
    this.brokerSettings = new BrokerSettings(configSection);
  }

  @Test
  void simpleName() {
    // GIVEN
    when(configSection.getString("names.simple"))
        .thenReturn("Simple Broker");

    // WHEN
    String simpleName = brokerSettings.getSimpleBrokerName();

    // THEN
    assertEquals("Simple Broker", simpleName);
  }

  @Test
  void isFeesOnSale() {
    // GIVEN
    when(configSection.getBoolean("charge-fees-on-sale"))
        .thenReturn(true);

    // WHEN
    boolean chargeOnSale = brokerSettings.isBrokerOnSale();

    // THEN
    assertTrue(chargeOnSale);
  }

  @Test
  void brokerPercent() {
    // GIVEN
    when(configSection.getBigDecimal("percent-fee"))
        .thenReturn(BigDecimal.valueOf(0.7));

    // WHEN
    BigDecimal brokerPercent = brokerSettings.getBrokerPercent();

    // THEN
    assertEquals(BigDecimal.valueOf(0.7), brokerPercent);
  }

  @Test
  void brokerPercentString() {
    // GIVEN
    when(configSection.getBigDecimal("percent-fee"))
        .thenReturn(BigDecimal.valueOf(0.7));

    // WHEN
    String brokerPercent = brokerSettings.getBrokerPercentString();

    // THEN
    assertEquals("70.0", brokerPercent);
  }

  @Test
  void brokerFlat() {
    // GIVEN
    when(configSection.getBigDecimal("flat-fee"))
        .thenReturn(BigDecimal.valueOf(50));

    // WHEN
    BigDecimal brokerFlat = brokerSettings.getBrokerFlat();

    // THEN
    assertEquals(BigDecimal.valueOf(50), brokerFlat);
  }

  @Test
  void brokerFlatString() {
    // GIVEN
    when(configSection.getBigDecimal("flat-fee"))
        .thenReturn(BigDecimal.valueOf(50));

    // WHEN
    String brokerFlat = brokerSettings.getBrokerFlatString();

    // THEN
    assertEquals("50", brokerFlat);
  }

  @Test
  void commandsDisabled() {
    // GIVEN
    when(configSection.getBoolean("disable-commands"))
        .thenReturn(true);

    // WHEN
    boolean isDisabled = brokerSettings.isCommandsDisabled();

    // THEN
    assertTrue(isDisabled);
  }
}
