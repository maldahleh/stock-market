package com.maldahleh.stockmarket.placeholder;

import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.placeholder.model.PlaceholderStock;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.CurrencyUtils;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import yahoofinance.Stock;

public class StockPlaceholderManager {

  private final Plugin plugin;
  private final StockManager stockManager;
  private final Settings settings;

  private final Map<String, PlaceholderStock> placeholderMap = new ConcurrentHashMap<>();
  private final Set<String> pendingOperations = ConcurrentHashMap.newKeySet();

  public StockPlaceholderManager(Plugin plugin, StockManager stockManager, Settings settings) {
    this.plugin = plugin;
    this.stockManager = stockManager;
    this.settings = settings;

    Bukkit.getScheduler()
        .runTaskTimerAsynchronously(
            plugin,
            () -> {
              for (Entry<String, PlaceholderStock> entry : placeholderMap.entrySet()) {
                buildPlaceholderStock(entry.getValue(), entry.getKey());
              }
            },
            20L,
            settings.getCacheMinutes() * 60L * 20L);
  }

  public PlaceholderStock getPlaceholderStock(String symbol) {
    String uppercaseSymbol = symbol.toUpperCase();
    PlaceholderStock placeholderStock = placeholderMap.get(uppercaseSymbol);
    if (placeholderStock != null) {
      return placeholderStock;
    }

    if (pendingOperations.contains(uppercaseSymbol)) {
      return null;
    }

    pendingOperations.add(uppercaseSymbol);
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              placeholderMap.put(
                  uppercaseSymbol,
                  buildPlaceholderStock(null, uppercaseSymbol)
              );
              pendingOperations.remove(uppercaseSymbol);
            });

    return null;
  }

  private PlaceholderStock buildPlaceholderStock(PlaceholderStock existing, String symbol) {
    Stock stock = stockManager.getStock(symbol);

    PlaceholderStock placeholder = determinePlaceholderStock(existing);
    placeholder.setStock(stock);
    placeholder.setServerPrice(CurrencyUtils.format(stockManager.getServerPrice(stock), settings));

    return placeholder;
  }

  private PlaceholderStock determinePlaceholderStock(PlaceholderStock existing) {
    if (existing != null) {
      return existing;
    }

    return new PlaceholderStock();
  }
}
