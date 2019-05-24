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

      BigDecimal price = stock.getQuote().getPrice().multiply(settings.getPriceMultiplier());
      if (!stock.getCurrency().equalsIgnoreCase("USD")) {
        BigDecimal conversionFactor = stockManager.getFxRate(stock.getCurrency());
        if (conversionFactor == null) {
          messages.sendInvalidStock(player);
          return;
        }

        price = price.multiply(conversionFactor);
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
                  .put("<market-price>", Utils.format(stock.getQuote().getPrice(),
                      settings.getUnknownData()))
                  .put("<market-currency>", stock.getCurrency())
                  .put("<server-price>", Utils.format(finalPrice, settings.getUnknownData()))
                  .put("<server-currency>", stockMarket.getEcon().currencyNamePlural())
                  .put("<broker-flat>", settings.getBrokerFlatString())
                  .put("<broker-percent>", settings.getBrokerPercentString())
                  .put("<change-close>", Utils.format(stock.getQuote().getChange(),
                      settings.getUnknownData()))
                  .put("<change-year-high>", Utils.format(stock.getQuote().getChangeFromYearHigh(),
                      settings.getUnknownData()))
                  .put("<change-year-low>", Utils.format(stock.getQuote().getChangeFromYearLow(),
                      settings.getUnknownData()))
                  .put("<change-50-moving-avg>", Utils.format(stock.getQuote().getChangeFromAvg50(),
                      settings.getUnknownData()))
                  .put("<change-200-moving-avg>", Utils.format(stock.getQuote()
                      .getChangeFromAvg200(), settings.getUnknownData()))
                  .put("<yield>", Utils.formatSingle(stock.getDividend().getAnnualYieldPercent(),
                      settings.getUnknownData()))
                  .put("<symbol>", stock.getSymbol().toUpperCase())
                  .put("<day-high>", Utils.format(stock.getQuote().getDayHigh(),
                      settings.getUnknownData()))
                  .put("<day-low>", Utils.format(stock.getQuote().getDayLow(),
                      settings.getUnknownData()))
                  .put("<open-price>", Utils.format(stock.getQuote().getOpen(),
                      settings.getUnknownData()))
                  .put("<volume>", Utils.sigFigNumber(stock.getQuote().getVolume()))
                  .put("<close-price>", Utils.format(stock.getQuote().getPreviousClose(),
                      settings.getUnknownData()))
                  .put("<year-high>", Utils.format(stock.getQuote().getYearHigh(),
                      settings.getUnknownData()))
                  .put("<year-low>", Utils.format(stock.getQuote().getYearLow(),
                      settings.getUnknownData()))
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
                .put("<day-open>", Utils.format(quote.getOpen(), settings.getUnknownData()))
                .put("<day-close>", Utils.format(quote.getClose(), settings.getUnknownData()))
                .put("<volume>", Utils.formatSigFig(quote.getVolume(), settings.getUnknownData()))
                .put("<day-high>", Utils.format(quote.getHigh(), settings.getUnknownData()))
                .put("<day-low>", Utils.format(quote.getLow(), settings.getUnknownData()))
                .build()));
          } catch (IndexOutOfBoundsException | IOException e) {
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
