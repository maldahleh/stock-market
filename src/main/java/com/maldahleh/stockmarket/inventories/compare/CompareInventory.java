package com.maldahleh.stockmarket.inventories.compare;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.inventories.utils.common.StockDataInventory;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.Utils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import yahoofinance.Stock;

public class CompareInventory extends StockDataInventory {

  private final int perStock;

  public CompareInventory(StockMarket stockMarket, StockManager stockManager, Messages messages,
      Settings settings, ConfigSection section) {
    super(stockMarket, stockManager, messages, settings, section);

    this.perStock = section.getInt("per-stock");
  }

  @Override
  protected Inventory buildInventory(List<Entry<Stock, BigDecimal>> stocks) {
    Inventory inventory = Bukkit.createInventory(null, perStock * stocks.size(), name);

    for (int index = 0; index < stocks.size(); index++) {
      for (String key : section.getSection("items").getKeys()) {
        int slot = Integer.parseInt(key) + (perStock * index);
        inventory.setItem(
            slot,
            Utils.createItemStack(
                section.getSection("items." + key),
                buildStockDataMap(
                    stocks.get(index).getKey(),
                    stocks.get(index).getValue()
                )
            )
        );
      }
    }

    return inventory;
  }
}
