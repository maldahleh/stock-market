package com.maldahleh.stockmarket.inventories.lookup;

import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.utils.common.StockDataInventory;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.StockDataUtils;
import com.maldahleh.stockmarket.utils.CurrencyUtils;
import com.maldahleh.stockmarket.utils.Utils;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import yahoofinance.Stock;
import yahoofinance.histquotes.HistoricalQuote;

public class LookupInventory extends StockDataInventory {

  private static final String DATE_FORMAT = "MMMM dd, yyyy";

  private final int inventorySize;

  private final ItemStack historicalStack;
  private final ItemStack noHistoricalStack;
  private final List<Integer> historicalSlots;

  public LookupInventory(
      StockMarket stockMarket,
      StockManager stockManager,
      Messages messages,
      Settings settings,
      ConfigurationSection section) {
    super(stockMarket, stockManager, messages, settings, section);

    this.inventorySize = section.getInt("inventory.size");

    this.historicalStack =
        Utils.createItemStack(section.getConfigurationSection("historical." + "data"));
    this.noHistoricalStack =
        Utils.createItemStack(section.getConfigurationSection("historical." + "no-data"));
    this.historicalSlots = section.getIntegerList("historical.slots");
  }

  @Override
  protected Inventory buildInventory(List<Entry<Stock, BigDecimal>> stocks) {
    Stock stock = stocks.get(0).getKey();
    BigDecimal price = stocks.get(0).getValue();

    Inventory inventory =
        Bukkit.createInventory(
            null,
            inventorySize,
            inventoryName.replace("<symbol>", stock.getSymbol().toUpperCase()));

    for (String key : section.getConfigurationSection("items").getKeys(false)) {
      inventory.setItem(
          Integer.parseInt(key),
          Utils.createItemStack(
              section.getConfigurationSection("items." + key),
              StockDataUtils.buildStockDataMap(stock, price,
                  stockMarket.getEcon().currencyNamePlural(), settings)));
    }

    for (int index = 0; index < historicalSlots.size(); index++) {
      Integer slot = historicalSlots.get(index);

      HistoricalQuote quote = getHistorical(index, stock);
      if (quote == null) {
        inventory.setItem(slot, noHistoricalStack);
        continue;
      }

        inventory.setItem(
            slot,
            Utils.updateItemStack(
                historicalStack.clone(),
                ImmutableMap.<String, Object>builder()
                    .put("<date>", formatDate(quote.getDate().getTime()))
                    .put("<market-currency>", stock.getCurrency())
                    .put("<day-open>",
                        CurrencyUtils.format(quote.getOpen(), settings))
                    .put("<day-close>",
                        CurrencyUtils.format(quote.getClose(), settings))
                    .put("<volume>",
                        CurrencyUtils.formatSigFig(
                            quote.getVolume(), settings.getUnknownData()))
                    .put("<day-high>",
                        CurrencyUtils.format(quote.getHigh(), settings))
                    .put("<day-low>",
                        CurrencyUtils.format(quote.getLow(), settings))
                    .build()));
    }

    return inventory;
  }

  private String formatDate(Date date) {
    DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, settings.getLocale());
    return dateFormat.format(date);
  }

  private HistoricalQuote getHistorical(int index, Stock stock) {
    try {
      return stock.getHistory().get(index);
    } catch (IndexOutOfBoundsException | IOException e) {
      return null;
    }
  }
}
