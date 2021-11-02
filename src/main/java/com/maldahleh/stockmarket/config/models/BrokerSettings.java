package com.maldahleh.stockmarket.config.models;

import com.maldahleh.stockmarket.config.common.ConfigSection;
import lombok.Getter;

@Getter
public class BrokerSettings {

  private final String simpleBrokerName;
  private final boolean commandsDisabled;

  public BrokerSettings(ConfigSection configSection) {
    this.simpleBrokerName = configSection.getString("names.simple");
    this.commandsDisabled = configSection.getBoolean("settings.disable-commands");
  }
}
