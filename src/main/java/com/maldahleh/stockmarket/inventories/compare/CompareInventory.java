package com.maldahleh.stockmarket.inventories.compare;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.utils.common.StockDataInventory;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.StockDataUtils;
import com.maldahleh.stockmarket.utils.Utils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import yahoofinance.Stock;

public class CompareInventory extends StockDataInventory {

  private final int perStock;

  public CompareInventory(
      StockMarket stockMarket,
      StockManager stockManager,
      Messages messages,
      Settings settings,
      ConfigurationSection section) {
    super(stockMarket, stockManager, messages, settings, section);

    this.perStock = section.getInt("inventory.per-stock");
  }

  @Override
  protected Inventory buildInventory(List<Entry<Stock, BigDecimal>> stocks) {
    Inventory inventory = Bukkit.createInventory(null, perStock * stocks.size(),
        inventoryName);

    for (int index = 0; index < stocks.size(); index++) {
      for (String key : section.getConfigurationSection("items").getKeys(false)) {
        int slot = Integer.parseInt(key) + (perStock * index);
        inventory.setItem(
            slot,
            Utils.createItemStack(
                section.getConfigurationSection("items." + key),
                StockDataUtils.buildStockDataMap(
                    stocks.get(index).getKey(),
                    stocks.get(index).getValue(),
                    stockMarket.getEcon().currencyNamePlural(),
                    settings
                )
            )
        );
      }
    }

    return inventory;
  }
}
