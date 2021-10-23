package com.maldahleh.stockmarket.inventories.compare;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.utils.common.StockDataInventory;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.StockDataUtils;
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

  public void openInventory(Player player, String... symbols) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            stockMarket,
            () -> {
              Map<Stock, BigDecimal> stockMap = new LinkedHashMap<>();
              for (String symbol : symbols) {
                Stock stock = stockManager.getStock(symbol);
                if (stockManager.canNotUseStock(player, stock, settings, messages)) {
                  return;
                }

                BigDecimal price = stockManager.getServerPrice(stock);
                if (price == null) {
                  messages.sendInvalidStock(player);
                  return;
                }

                stockMap.put(stock, price);
              }

              List<Entry<Stock, BigDecimal>> stocks = new ArrayList<>(stockMap.entrySet());
              Bukkit.getScheduler()
                  .runTask(
                      stockMarket,
                      () -> {
                        Inventory inventory =
                            Bukkit.createInventory(null, perStock * stocks.size(), inventoryName);

                        for (int index = 0; index < stocks.size(); index++) {
                          Map.Entry<Stock, BigDecimal> entry = stocks.get(index);
                          if (entry == null) {
                            continue;
                          }

                          Stock stock = entry.getKey();
                          for (String key :
                              section.getConfigurationSection("items").getKeys(false)) {
                            int slot = Integer.parseInt(key) + (perStock * index);
                            inventory.setItem(
                                slot,
                                Utils.createItemStack(
                                    section.getConfigurationSection("items." + key),
                                    StockDataUtils.buildStockDataMap(stock, entry.getValue(),
                                        stockMarket.getEcon().currencyNamePlural(), settings)));
                          }
                        }

                        player.openInventory(inventory);
                        addViewer(player);
                      });
            });
  }
}
