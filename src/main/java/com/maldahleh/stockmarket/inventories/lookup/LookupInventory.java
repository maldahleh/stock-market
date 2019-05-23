package com.maldahleh.stockmarket.inventories.lookup;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.lookup.listeners.LookupListener;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.Utils;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import yahoofinance.Stock;
import yahoofinance.histquotes.HistoricalQuote;

public class LookupInventory {
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
  private final Set<UUID> activeViewers;

  public LookupInventory(StockMarket stockMarket, StockManager stockManager, Messages messages,
      Settings settings, ConfigurationSection section) {
    this.stockMarket = stockMarket;
    this.stockManager = stockManager;
    this.messages = messages;
    this.settings = settings;
    this.section = section;

    this.inventoryName = Utils.color(section.getString("inventory.name"));
    this.inventorySize = section.getInt("inventory.size");

    this.historicalStack = Utils.createItemStack(section.getConfigurationSection("historical."
        + "data"));
    this.noHistoricalStack = Utils.createItemStack(section.getConfigurationSection("historical."
        + "no-data"));
    this.historicalSlots = section.getIntegerList("historical.slots");

    this.useCache = section.getBoolean("cache.use-cache");
    if (useCache) {
      this.inventoryCache = CacheBuilder.newBuilder().maximumSize(500)
          .expireAfterWrite(section.getInt("cache.cache-minutes"), TimeUnit.MINUTES).build();
    } else {
      this.inventoryCache = CacheBuilder.newBuilder().build();
    }

    this.activeViewers = new HashSet<>();

    Bukkit.getServer().getPluginManager().registerEvents(new LookupListener(this), stockMarket);
  }

  public void openInventory(Player player, String symbol) {
    Inventory cachedInventory = inventoryCache.getIfPresent(symbol.toUpperCase());
    if (cachedInventory != null) {
      player.openInventory(cachedInventory);
      activeViewers.add(player.getUniqueId());
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(stockMarket, () -> {
      Stock stock = stockManager.getStock(symbol);
      if (stock == null || stock.getName().equalsIgnoreCase("N/A")) {
        messages.sendInvalidStock(player);
        return;
      }

      if (!settings.isAllowedCurrency(stock.getCurrency())
          || !settings.isAllowedExchange(stock.getStockExchange())) {
        messages.sendDisabledStock(player);
        return;
      }

      if (!settings.isAboveMinimumPrice(stock.getQuote().getPrice())) {
        messages.sendLowPriceStock(player);
        return;
      }

      BigDecimal price = stock.getQuote().getPrice();
      if (!stock.getCurrency().equalsIgnoreCase("USD")) {
        BigDecimal conversionFactor = stockManager.getFxRate(stock.getCurrency());
        if (conversionFactor == null) {
          messages.sendInvalidStock(player);
          return;
        }

        price = price.multiply(settings.getBrokerPercent()).multiply(conversionFactor)
            .multiply(settings.getPriceMultiplier());
      }

      BigDecimal finalPrice = price;
      Bukkit.getScheduler().runTask(stockMarket, () -> {
        Inventory inventory = Bukkit.createInventory(null, inventorySize, inventoryName
            .replace("<symbol>", stock.getSymbol().toUpperCase()));

        for (String key : section.getConfigurationSection("items").getKeys(false)) {
          inventory.setItem(Integer.valueOf(key),
              Utils.createItemStack(section.getConfigurationSection("items." + key), ImmutableMap
                  .<String, Object>builder()
                  .put("<name>", stock.getName())
                  .put("<exchange>", stock.getStockExchange())
                  .put("<cap>", Utils.sigFigNumber(stock.getStats().getMarketCap().doubleValue()))
                  .put("<market-price>", stock.getQuote().getPrice().toPlainString())
                  .put("<market-currency>", stock.getCurrency())
                  .put("<server-price>", finalPrice.toPlainString())
                  .put("<server-currency>", stockMarket.getEcon().currencyNamePlural())
                  .put("<broker-flat>", settings.getBrokerFlatString())
                  .put("<broker-percent>", settings.getBrokerPercentString())
                  .put("<change-close>", stockMarket.getEcon().format(stock.getQuote().getChange()
                      .doubleValue()))
                  .put("<change-year-high>", stockMarket.getEcon().format(stock.getQuote()
                      .getChangeFromYearHigh().doubleValue()))
                  .put("<change-year-low>", stockMarket.getEcon().format(stock.getQuote()
                      .getChangeFromYearLow().doubleValue()))
                  .put("<change-50-moving-avg>", stockMarket.getEcon().format(stock.getQuote()
                      .getChangeFromAvg50().doubleValue()))
                  .put("<change-200-moving-avg>", stockMarket.getEcon().format(stock.getQuote()
                      .getChangeFromAvg200().doubleValue()))
                  .put("<yield>", stockMarket.getEcon().format(stock.getQuote()
                      .getChangeFromAvg50().doubleValue()))
                  .put("<symbol>", stock.getSymbol().toUpperCase())
                  .put("<day-high>", stockMarket.getEcon().format(stock.getQuote()
                      .getDayHigh().doubleValue()))
                  .put("<day-low>", stockMarket.getEcon().format(stock.getQuote()
                      .getDayLow().doubleValue()))
                  .put("<open-price>", stockMarket.getEcon().format(stock.getQuote()
                      .getOpen().doubleValue()))
                  .put("<volume>", Utils.sigFigNumber(stock.getQuote().getVolume()))
                  .put("<close-price>", stockMarket.getEcon().format(stock.getQuote()
                      .getPreviousClose().doubleValue()))
                  .put("<year-high>", stockMarket.getEcon().format(stock.getQuote()
                      .getYearHigh().doubleValue()))
                  .put("<year-low>", stockMarket.getEcon().format(stock.getQuote()
                      .getYearLow().doubleValue()))
                  .build()));
        }

        for (int index = 0; index < historicalSlots.size(); index++) {
          Integer slot = historicalSlots.get(index);
          try {
            HistoricalQuote quote = stock.getHistory().get(index);
            inventory.setItem(slot, Utils.updateItemStack(historicalStack.clone(), ImmutableMap
                .<String, Object>builder()
                .put("<date>", quote.getDate().getTime().toString())
                .put("<market-currency>", stock.getCurrency())
                .put("<day-open>", stockMarket.getEcon().format(quote.getOpen().doubleValue()))
                .put("<day-close>", stockMarket.getEcon().format(quote.getClose().doubleValue()))
                .put("<volume>", Utils.sigFigNumber(quote.getVolume()))
                .put("<day-high>", stockMarket.getEcon().format(quote.getHigh().doubleValue()))
                .put("<day-low>", stockMarket.getEcon().format(quote.getLow().doubleValue()))
                .build()));
          } catch (IndexOutOfBoundsException | IOException | NullPointerException e) {
            inventory.setItem(slot, noHistoricalStack);
          }
        }

        player.openInventory(inventory);
        activeViewers.add(player.getUniqueId());

        if (useCache) {
          inventoryCache.put(symbol.toUpperCase(), inventory);
        }
      });
    });
  }

  public boolean hasActiveInventory(HumanEntity entity) {
    return activeViewers.contains(entity.getUniqueId());
  }

  public void remove(HumanEntity entity) {
    activeViewers.remove(entity.getUniqueId());
  }
}
