package com.maldahleh.stockmarket.stocks;

import com.google.common.cache.Cache;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.stocks.utils.StockUtils;
import com.maldahleh.stockmarket.placeholder.model.PlaceholderStock;
import com.maldahleh.stockmarket.utils.CurrencyUtils;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import yahoofinance.Stock;
import yahoofinance.quotes.fx.FxQuote;

public class StockManager {

  private static final String USD = "USD";

  private final Cache<String, Stock> stockCache;
  private final Cache<String, FxQuote> fxCache;

  private final Plugin plugin;
  private final Settings settings;
  private final MarketStatusProvider marketStatusProvider;

  private final Map<String, PlaceholderStock> placeholderMap = new ConcurrentHashMap<>();
  private final Set<String> pendingOperations = ConcurrentHashMap.newKeySet();

  public StockManager(Plugin plugin, ConfigurationSection section, Settings settings) {
    this.stockCache = StockUtils.buildCache(section);
    this.fxCache = StockUtils.buildCache(section);

    this.plugin = plugin;
    this.settings = settings;
    this.marketStatusProvider = new MarketStatusProvider(settings);

    Bukkit.getScheduler()
        .runTaskTimerAsynchronously(
            plugin,
            () -> {
              for (Entry<String, PlaceholderStock> entry : placeholderMap.entrySet()) {
                Stock stock = getStock(entry.getKey());

                PlaceholderStock placeholder = entry.getValue();
                placeholder.setStock(stock);
                placeholder.setServerPrice(CurrencyUtils.format(getServerPrice(stock), settings));
              }
            },
            20L,
            section.getInt("cache.expire-minutes") * 60L * 20L);
  }

  public void cacheStocks(String... symbols) {
    Map<String, Stock> results = StockUtils.fetchStocks(symbols);
    for (Map.Entry<String, Stock> e : results.entrySet()) {
      stockCache.put(e.getKey().toUpperCase(), e.getValue());
    }
  }

  public Stock getStock(String symbol) {
    String upperSymbol = symbol.toUpperCase();
    Stock stock = stockCache.getIfPresent(upperSymbol);
    if (stock != null) {
      return stock;
    }

    Stock fetchedStock = StockUtils.fetchStock(upperSymbol);
    if (fetchedStock == null) {
      return null;
    }

    stockCache.put(upperSymbol, fetchedStock);
    return fetchedStock;
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
              Stock stock = getStock(uppercaseSymbol);
              placeholderMap.put(
                  uppercaseSymbol,
                  new PlaceholderStock(stock, CurrencyUtils.format(getServerPrice(stock), settings))
              );
              pendingOperations.remove(uppercaseSymbol);
            });

    return null;
  }

  private BigDecimal getFxRate(String fxSymbol) {
    String fxQuote = fxSymbol.toUpperCase() + USD + "=X";
    FxQuote quote = fxCache.getIfPresent(fxQuote);
    if (quote != null) {
      return quote.getPrice();
    }

    FxQuote fetchedQuote = StockUtils.fetchFxQuote(fxQuote);
    if (fetchedQuote == null) {
      return null;
    }

    fxCache.put(fxQuote, fetchedQuote);
    return fetchedQuote.getPrice();
  }

  public BigDecimal getServerPrice(String symbol) {
    Stock stock = getStock(symbol);
    if (stock == null) {
      return null;
    }

    return getServerPrice(stock);
  }

  public BigDecimal getServerPrice(Stock stock) {
    BigDecimal price = stock.getQuote().getPrice().multiply(settings.getPriceMultiplier());
    if (stock.getCurrency().equalsIgnoreCase(USD)) {
      return price;
    }

    BigDecimal conversionFactor = getFxRate(stock.getCurrency());
    if (conversionFactor == null) {
      return null;
    }

    return price.multiply(conversionFactor);
  }

  public boolean canNotUseStock(Player player, Stock stock, Settings settings, Messages messages) {
    if (stock == null || stock.getName().equalsIgnoreCase("N/A")) {
      messages.sendInvalidStock(player);
      return true;
    }

    if (!settings.isAllowedCurrency(stock.getCurrency())
        || !settings.isAllowedExchange(stock.getStockExchange())) {
      messages.sendDisabledStock(player);
      return true;
    }

    if (!settings.isAboveMinimumPrice(stock.getQuote().getPrice())) {
      messages.sendLowPriceStock(player);
      return true;
    }

    if (settings.isBlockTransactionsWhenClosed()
        && !marketStatusProvider.isMarketOpen(stock.getSymbol())) {
      messages.sendMarketClosed(player);
      return true;
    }

    return false;
  }
}
