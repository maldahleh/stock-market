package com.maldahleh.stockmarket.config;

import com.maldahleh.stockmarket.utils.Utils;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class Messages {
  private final String noPermission;

  public Messages(ConfigurationSection section) {
    this.noPermission = Utils.color(section.getString("no-permission"));
  }
}
