package com.maldahleh.stockmarket.inventories.utils.common;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.Utils;
import org.bukkit.configuration.ConfigurationSection;

public abstract class StockDataInventory extends StockInventory {

  protected final StockMarket stockMarket;
  protected final StockManager stockManager;
  protected final Messages messages;
  protected final Settings settings;
  protected final ConfigurationSection section;

  protected final String inventoryName;

  protected StockDataInventory(
      StockMarket stockMarket,
      StockManager stockManager,
      Messages messages,
      Settings settings,
      ConfigurationSection section) {
    super(stockMarket);

    this.stockMarket = stockMarket;
    this.stockManager = stockManager;
    this.messages = messages;
    this.settings = settings;
    this.section = section;

    this.inventoryName = Utils.color(section.getString("inventory.name"));
  }
}
