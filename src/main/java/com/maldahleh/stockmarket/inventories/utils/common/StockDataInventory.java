package com.maldahleh.stockmarket.inventories.utils.common;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.Utils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import yahoofinance.Stock;

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

  protected abstract Inventory buildInventory(List<Entry<Stock, BigDecimal>> stocks);

  public void openInventory(Player player, String... symbols) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            stockMarket,
            () -> {
              Map<Stock, BigDecimal> stockMap = lookupStocks(player, symbols);

              List<Entry<Stock, BigDecimal>> stocks = new ArrayList<>(stockMap.entrySet());
              Bukkit.getScheduler()
                  .runTask(
                      stockMarket,
                      () -> {
                        Inventory inventory = buildInventory(stocks);

                        player.openInventory(inventory);
                        addViewer(player);
                      });
            });
  }

  private Map<Stock, BigDecimal> lookupStocks(Player player, String... symbols) {
    Map<Stock, BigDecimal> stockMap = new LinkedHashMap<>();
    for (String symbol : symbols) {
      Stock stock = stockManager.getStock(symbol);
      if (stockManager.canNotUseStock(player, stock)) {
        return new LinkedHashMap<>();
      }

      BigDecimal price = stockManager.getServerPrice(stock);
      if (price == null) {
        messages.sendInvalidStock(player);
        return new LinkedHashMap<>();
      }

      stockMap.put(stock, price);
    }

    return stockMap;
  }
}
