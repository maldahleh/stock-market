package com.maldahleh.stockmarket.inventories.utils.common;

import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.CurrencyUtils;
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

  protected Map<String, Object> buildStockDataMap(Stock stock, BigDecimal serverPrice) {
    return ImmutableMap.<String, Object>builder()
        .put("<name>", stock.getName())
        .put("<exchange>", stock.getStockExchange())
        .put("<cap>", CurrencyUtils.sigFigNumber(stock.getStats().getMarketCap()))
        .put("<market-price>", CurrencyUtils.format(stock.getQuote().getPrice(), settings))
        .put("<market-currency>", stock.getCurrency())
        .put("<server-price>", CurrencyUtils.format(serverPrice, settings))
        .put("<server-currency>", stockMarket.getEcon().currencyNamePlural())
        .put("<broker-flat>", settings.getBrokerSettings().getBrokerFlatString())
        .put("<broker-percent>", settings.getBrokerSettings().getBrokerPercentString())
        .put("<change-close>", CurrencyUtils.format(stock.getQuote().getChange(), settings))
        .put("<change-year-high>",
            CurrencyUtils.format(stock.getQuote().getChangeFromYearHigh(), settings))
        .put("<change-year-low>",
            CurrencyUtils.format(stock.getQuote().getChangeFromYearLow(), settings))
        .put("<change-50-moving-avg>",
            CurrencyUtils.format(stock.getQuote().getChangeFromAvg50(), settings))
        .put("<change-200-moving-avg>",
            CurrencyUtils.format(stock.getQuote().getChangeFromAvg200(), settings))
        .put("<yield>",
            CurrencyUtils.formatSingle(stock.getDividend().getAnnualYieldPercent(), settings))
        .put("<symbol>", stock.getSymbol().toUpperCase())
        .put("<day-high>", CurrencyUtils.format(stock.getQuote().getDayHigh(), settings))
        .put("<day-low>", CurrencyUtils.format(stock.getQuote().getDayLow(), settings))
        .put("<open-price>", CurrencyUtils.format(stock.getQuote().getOpen(), settings))
        .put("<volume>", CurrencyUtils.sigFigNumber(stock.getQuote().getVolume()))
        .put("<close-price>", CurrencyUtils.format(stock.getQuote().getPreviousClose(), settings))
        .put("<year-high>", CurrencyUtils.format(stock.getQuote().getYearHigh(), settings))
        .put("<year-low>", CurrencyUtils.format(stock.getQuote().getYearLow(), settings))
        .build();
  }
}
