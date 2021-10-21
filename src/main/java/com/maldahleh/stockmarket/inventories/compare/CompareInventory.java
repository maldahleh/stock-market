package com.maldahleh.stockmarket.inventories.compare;

import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.utils.common.StockInventory;
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

public class CompareInventory extends StockInventory {
  private final StockMarket stockMarket;
  private final StockManager stockManager;
  private final Messages messages;
  private final Settings settings;
  private final ConfigurationSection section;

  private final String inventoryName;
  private final int perStock;

  public CompareInventory(
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
                                    ImmutableMap.<String, Object>builder()
                                        .put("<name>", stock.getName())
                                        .put("<exchange>", stock.getStockExchange())
                                        .put(
                                            "<cap>",
                                            Utils.sigFigNumber(
                                                stock.getStats().getMarketCap().doubleValue()))
                                        .put(
                                            "<market-price>",
                                            Utils.format(
                                                stock.getQuote().getPrice(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put("<market-currency>", stock.getCurrency())
                                        .put(
                                            "<server-price>",
                                            Utils.format(
                                                entry.getValue(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<server-currency>",
                                            stockMarket.getEcon().currencyNamePlural())
                                        .put("<broker-flat>", settings.getBrokerFlatString())
                                        .put("<broker-percent>", settings.getBrokerPercentString())
                                        .put(
                                            "<change-close>",
                                            Utils.format(
                                                stock.getQuote().getChange(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<change-year-high>",
                                            Utils.format(
                                                stock.getQuote().getChangeFromYearHigh(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<change-year-low>",
                                            Utils.format(
                                                stock.getQuote().getChangeFromYearLow(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<change-50-moving-avg>",
                                            Utils.format(
                                                stock.getQuote().getChangeFromAvg50(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<change-200-moving-avg>",
                                            Utils.format(
                                                stock.getQuote().getChangeFromAvg200(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<yield>",
                                            Utils.formatSingle(
                                                stock.getDividend().getAnnualYieldPercent(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put("<symbol>", stock.getSymbol().toUpperCase())
                                        .put(
                                            "<day-high>",
                                            Utils.format(
                                                stock.getQuote().getDayHigh(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<day-low>",
                                            Utils.format(
                                                stock.getQuote().getDayLow(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<open-price>",
                                            Utils.format(
                                                stock.getQuote().getOpen(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<volume>",
                                            Utils.sigFigNumber(stock.getQuote().getVolume()))
                                        .put(
                                            "<close-price>",
                                            Utils.format(
                                                stock.getQuote().getPreviousClose(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<year-high>",
                                            Utils.format(
                                                stock.getQuote().getYearHigh(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<year-low>",
                                            Utils.format(
                                                stock.getQuote().getYearLow(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .build()));
                          }
                        }

                        player.openInventory(inventory);
                        addViewer(player);
                      });
            });
  }
}
