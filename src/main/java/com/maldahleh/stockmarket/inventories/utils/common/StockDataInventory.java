package com.maldahleh.stockmarket.inventories.utils.common;

import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.stocks.StockManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import yahoofinance.Stock;

public abstract class StockDataInventory extends StockInventory {

  protected final StockMarket stockMarket;
  protected final StockManager stockManager;
  protected final Messages messages;
  protected final Settings settings;
  protected final ConfigSection section;

  protected final String name;

  protected StockDataInventory(
      StockMarket stockMarket,
      StockManager stockManager,
      Messages messages,
      Settings settings,
      ConfigSection section) {
    super(stockMarket);

    this.stockMarket = stockMarket;
    this.stockManager = stockManager;
    this.messages = messages;
    this.settings = settings;
    this.section = section;

    this.name = section.getString("name");
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
        .put("<cap>", settings.formatSigFig(stock.getStats().getMarketCap()))
        .put("<market-price>", settings.format(stock.getQuote().getPrice()))
        .put("<market-currency>", stock.getCurrency())
        .put("<server-price>", settings.format(serverPrice))
        .put("<server-currency>", stockMarket.getEcon().currencyNamePlural())
        .put("<broker-flat>", settings.getBrokerSettings().getBrokerFlatString())
        .put("<broker-percent>", settings.getBrokerSettings().getBrokerPercentString())
        .put("<change-close>", settings.format(stock.getQuote().getChange()))
        .put("<change-year-high>", settings.format(stock.getQuote().getChangeFromYearHigh()))
        .put("<change-year-low>", settings.format(stock.getQuote().getChangeFromYearLow()))
        .put("<change-50-moving-avg>", settings.format(stock.getQuote().getChangeFromAvg50()))
        .put("<change-200-moving-avg>", settings.format(stock.getQuote().getChangeFromAvg200()))
        .put("<yield>", settings.formatSingle(stock.getDividend().getAnnualYieldPercent()))
        .put("<symbol>", stock.getSymbol().toUpperCase())
        .put("<day-high>", settings.format(stock.getQuote().getDayHigh()))
        .put("<day-low>", settings.format(stock.getQuote().getDayLow()))
        .put("<open-price>", settings.format(stock.getQuote().getOpen()))
        .put("<volume>", settings.formatSigFig(stock.getQuote().getVolume()))
        .put("<close-price>", settings.format(stock.getQuote().getPreviousClose()))
        .put("<year-high>", settings.format(stock.getQuote().getYearHigh()))
        .put("<year-low>", settings.format(stock.getQuote().getYearLow()))
        .build();
  }
}
