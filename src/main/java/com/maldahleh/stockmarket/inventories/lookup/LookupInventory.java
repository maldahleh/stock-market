package com.maldahleh.stockmarket.inventories.lookup;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.inventories.utils.common.StockDataInventory;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.Utils;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import yahoofinance.Stock;
import yahoofinance.histquotes.HistoricalQuote;

public class LookupInventory extends StockDataInventory {

  private static final String DATE_FORMAT = "MMMM dd, yyyy";

  private final int size;

  private final ItemStack historicalStack;
  private final ItemStack noHistoricalStack;
  private final List<Integer> historicalSlots;

  public LookupInventory(StockMarket stockMarket, StockManager stockManager, Messages messages,
      Settings settings, ConfigSection section) {
    super(stockMarket, stockManager, messages, settings, section);

    this.size = section.getInt("size");

    this.historicalStack = section.getItemStack("historical.data");
    this.noHistoricalStack = section.getItemStack("historical.no-data");
    this.historicalSlots = section.getIntegerList("historical.slots");
  }

  @Override
  protected Inventory buildInventory(List<Entry<Stock, BigDecimal>> stocks) {
    Stock stock = stocks.get(0).getKey();
    BigDecimal price = stocks.get(0).getValue();

    Inventory inventory = Bukkit.createInventory(null, size,
        name.replace("<symbol>", stock.getSymbol().toUpperCase()));

    for (String key : section.getSection("items").getKeys()) {
      inventory.setItem(
          Integer.parseInt(key),
          Utils.createItemStack(
              section.getSection("items." + key),
              buildStockDataMap(stock, price)
          )
      );
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
              historicalStack,
              Map.of(
                  "<date>", formatDate(quote.getDate().getTime()),
                  "<market-currency>", stock.getCurrency(),
                  "<day-open>", settings.format(quote.getOpen()),
                  "<day-close>", settings.format(quote.getClose()),
                  "<volume>", settings.formatSigFig(quote.getVolume()),
                  "<day-high>", settings.format(quote.getHigh()),
                  "<day-low>", settings.format(quote.getLow())
              )
          )
      );
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
