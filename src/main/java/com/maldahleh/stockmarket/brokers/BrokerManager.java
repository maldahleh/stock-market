package com.maldahleh.stockmarket.brokers;

import com.maldahleh.stockmarket.utils.Utils;
import org.bukkit.configuration.ConfigurationSection;

public class BrokerManager {
  private String simpleBrokerName;
  private String complexBrokerName;

  public BrokerManager(ConfigurationSection section) {
    this.simpleBrokerName = Utils.color(section.getString("names.simple"));
    this.complexBrokerName = Utils.color(section.getString("names.complex"));
  }

}
