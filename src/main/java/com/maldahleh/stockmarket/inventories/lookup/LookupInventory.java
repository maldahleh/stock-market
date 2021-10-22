package com.maldahleh.stockmarket.inventories.lookup;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.utils.common.StockInventory;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.TimeUtils;
import com.maldahleh.stockmarket.utils.CurrencyUtils;
import com.maldahleh.stockmarket.utils.Utils;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import yahoofinance.Stock;
import yahoofinance.histquotes.HistoricalQuote;

public class LookupInventory extends StockInventory {

  private final StockMarket stockMarket;
  private final StockManager stockManager;
  private final Messages messages;
  private final Settings settings;
  private final ConfigurationSection section;

  private final String inventoryName;
  private final int inventorySize;

  private final ItemStack historicalStack;
  private final ItemStack noHistoricalStack;
  private final List<Integer> historicalSlots;

  private final boolean useCache;
  private final Cache<String, Inventory> inventoryCache;

  public LookupInventory(
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
    this.inventorySize = section.getInt("inventory.size");

    this.historicalStack =
        Utils.createItemStack(section.getConfigurationSection("historical." + "data"));
    this.noHistoricalStack =
        Utils.createItemStack(section.getConfigurationSection("historical." + "no-data"));
    this.historicalSlots = section.getIntegerList("historical.slots");

    this.useCache = section.getBoolean("cache.use-cache");
    if (useCache) {
      this.inventoryCache =
          CacheBuilder.newBuilder()
              .maximumSize(500)
              .expireAfterWrite(section.getInt("cache.cache-minutes"), TimeUnit.MINUTES)
              .build();
    } else {
      this.inventoryCache = CacheBuilder.newBuilder().build();
    }
  }

  public void openInventory(Player player, String symbol) {
    Inventory cachedInventory = inventoryCache.getIfPresent(symbol.toUpperCase());
    if (cachedInventory != null) {
      player.openInventory(cachedInventory);
      addViewer(player);
      return;
    }

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            stockMarket,
            () -> {
              Stock stock = stockManager.getStock(symbol);
              if (stockManager.canNotUseStock(player, stock, settings, messages)) {
                return;
              }

              BigDecimal price = stockManager.getServerPrice(stock);
              if (price == null) {
                messages.sendInvalidStock(player);
                return;
              }

              Bukkit.getScheduler()
                  .runTask(
                      stockMarket,
                      () -> {
                        Inventory inventory =
                            Bukkit.createInventory(
                                null,
                                inventorySize,
                                inventoryName.replace("<symbol>", stock.getSymbol().toUpperCase()));

                        for (String key : section.getConfigurationSection("items").getKeys(false)) {
                          inventory.setItem(
                              Integer.valueOf(key),
                              Utils.createItemStack(
                                  section.getConfigurationSection("items." + key),
                                  ImmutableMap.<String, Object>builder()
                                      .put("<name>", stock.getName())
                                      .put("<exchange>", stock.getStockExchange())
                                      .put(
                                          "<cap>",
                                          CurrencyUtils.sigFigNumber(
                                              stock.getStats().getMarketCap().doubleValue()))
                                      .put(
                                          "<market-price>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getPrice(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put("<market-currency>", stock.getCurrency())
                                      .put(
                                          "<server-price>",
                                          CurrencyUtils.format(
                                              price,
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put(
                                          "<server-currency>",
                                          stockMarket.getEcon().currencyNamePlural())
                                      .put("<broker-flat>", settings.getBrokerFlatString())
                                      .put("<broker-percent>", settings.getBrokerPercentString())
                                      .put(
                                          "<change-close>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getChange(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put(
                                          "<change-year-high>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getChangeFromYearHigh(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put(
                                          "<change-year-low>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getChangeFromYearLow(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put(
                                          "<change-50-moving-avg>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getChangeFromAvg50(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put(
                                          "<change-200-moving-avg>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getChangeFromAvg200(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put(
                                          "<yield>",
                                          CurrencyUtils.formatSingle(
                                              stock.getDividend().getAnnualYieldPercent(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put("<symbol>", stock.getSymbol().toUpperCase())
                                      .put(
                                          "<day-high>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getDayHigh(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put(
                                          "<day-low>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getDayLow(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put(
                                          "<open-price>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getOpen(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put(
                                          "<volume>",
                                          CurrencyUtils.sigFigNumber(stock.getQuote().getVolume()))
                                      .put(
                                          "<close-price>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getPreviousClose(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put(
                                          "<year-high>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getYearHigh(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .put(
                                          "<year-low>",
                                          CurrencyUtils.format(
                                              stock.getQuote().getYearLow(),
                                              settings.getUnknownData(),
                                              settings.getLocale()))
                                      .build()));
                        }

                        for (int index = 0; index < historicalSlots.size(); index++) {
                          Integer slot = historicalSlots.get(index);
                          try {
                            HistoricalQuote quote = stock.getHistory().get(index);
                            inventory.setItem(
                                slot,
                                Utils.updateItemStack(
                                    historicalStack.clone(),
                                    ImmutableMap.<String, Object>builder()
                                        .put(
                                            "<date>",
                                            TimeUtils.formatDate(
                                                quote.getDate().getTime(), settings.getLocale()))
                                        .put("<market-currency>", stock.getCurrency())
                                        .put(
                                            "<day-open>",
                                            CurrencyUtils.format(
                                                quote.getOpen(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<day-close>",
                                            CurrencyUtils.format(
                                                quote.getClose(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<volume>",
                                            CurrencyUtils.formatSigFig(
                                                quote.getVolume(), settings.getUnknownData()))
                                        .put(
                                            "<day-high>",
                                            CurrencyUtils.format(
                                                quote.getHigh(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .put(
                                            "<day-low>",
                                            CurrencyUtils.format(
                                                quote.getLow(),
                                                settings.getUnknownData(),
                                                settings.getLocale()))
                                        .build()));
                          } catch (IndexOutOfBoundsException | IOException e) {
                            inventory.setItem(slot, noHistoricalStack);
                          }
                        }

                        player.openInventory(inventory);
                        addViewer(player);

                        if (useCache) {
                          inventoryCache.put(symbol.toUpperCase(), inventory);
                        }
                      });
            });
  }
}
