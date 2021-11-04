package com.maldahleh.stockmarket.config.models;

import com.maldahleh.stockmarket.config.common.ConfigSection;
import java.math.BigDecimal;

public record BrokerSettings(ConfigSection configSection) {

  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  public String getSimpleBrokerName() {
    return configSection.getString("names.simple");
  }

  public boolean isBrokerOnSale() {
    return configSection.getBoolean("charge-fees-on-sale");
  }

  public BigDecimal getBrokerPercent() {
    return configSection.getBigDecimal("percent-fee");
  }

  public String getBrokerPercentString() {
    return getBrokerPercent().multiply(ONE_HUNDRED).toPlainString();
  }

  public BigDecimal getBrokerFlat() {
    return configSection.getBigDecimal("flat-fee");
  }

  public String getBrokerFlatString() {
    return getBrokerFlat().toPlainString();
  }

  public boolean isCommandsDisabled() {
    return configSection.getBoolean("disable-commands");
  }
}
