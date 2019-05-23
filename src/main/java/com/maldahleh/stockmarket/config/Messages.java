package com.maldahleh.stockmarket.config;

import com.maldahleh.stockmarket.utils.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Messages {
  private final String lowPriceStock;
  private final String disabledStock;
  private final String invalidStock;
  private final String noPermission;

  public Messages(ConfigurationSection section) {
    this.lowPriceStock = Utils.color(section.getString("low-price-stock"));
    this.disabledStock = Utils.color(section.getString("disabled-stock"));
    this.invalidStock = Utils.color(section.getString("invalid-stock"));
    this.noPermission = Utils.color(section.getString("no-permission"));
  }

  public void sendLowPriceStock(Player player) {
    player.sendMessage(lowPriceStock);
  }

  public void sendDisabledStock(Player player) {
    player.sendMessage(disabledStock);
  }

  public void sendInvalidStock(Player player) {
    player.sendMessage(invalidStock);
  }

  public void sendNoPermission(Player player) {
    player.sendMessage(noPermission);
  }
}
