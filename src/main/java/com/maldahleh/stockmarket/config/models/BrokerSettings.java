package com.maldahleh.stockmarket.config.models;

import com.maldahleh.stockmarket.config.common.ConfigSection;

public record BrokerSettings(ConfigSection configSection) {

  public String getSimpleBrokerName() {
    return configSection.getString("names.simple");
  }

  public boolean isCommandsDisabled() {
    return configSection.getBoolean("settings.disable-commands");
  }
}
