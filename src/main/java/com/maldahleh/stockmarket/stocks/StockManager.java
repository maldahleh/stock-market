package com.maldahleh.stockmarket.stocks;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.stocks.wrapper.PlaceholderStock;
import com.maldahleh.stockmarket.utils.Utils;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.fx.FxQuote;

public class StockManager {
  private final Cache<String, Stock> stockCache;
  private final Cache<String, Boolean> marketOpenCache;
  private final Cache<String, FxQuote> fxCache;

  private final Map<String, PlaceholderStock> placeholderMap;
  private final Set<String> pendingOperations;

  private final Plugin plugin;
  private final Settings settings;

  public StockManager(Plugin plugin, ConfigurationSection section, Settings settings) {
    this.stockCache = CacheBuilder.newBuilder().expireAfterWrite(section
        .getInt("cache.expire-minutes"), TimeUnit.MINUTES).maximumSize(500).build();
    this.marketOpenCache = CacheBuilder.newBuilder().expireAfterWrite(section
        .getInt("cache.expire-minutes"), TimeUnit.MINUTES).maximumSize(500).build();
    this.fxCache = CacheBuilder.newBuilder().expireAfterWrite(section
        .getInt("cache.expire-minutes"), TimeUnit.MINUTES).maximumSize(500).build();

    this.placeholderMap = new ConcurrentHashMap<>();
    this.pendingOperations = ConcurrentHashMap.newKeySet();

    this.plugin = plugin;
    this.settings = settings;

    Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
      for (Entry<String, PlaceholderStock> entry : placeholderMap.entrySet()) {
        Stock stock = getStock(entry.getKey());
        entry.getValue().setStock(stock);
        entry.getValue().setServerPrice(Utils.format(getServerPrice(stock),
            settings.getUnknownData(), settings.getLocale()));
      }
    }, 20L, section.getInt("cache.expire-minutes") * 60L * 20L);
  }

  public void cacheStocks(String... symbols) {
    try {
      Map<String, Stock> results = YahooFinance.get(symbols);
      for (Map.Entry<String, Stock> e : results.entrySet()) {
        if (e.getKey() == null || e.getValue() == null) {
          continue;
        }

        stockCache.put(e.getKey().toUpperCase(), e.getValue());
      }
    } catch (IOException ignored) {}
  }

  public Stock getStock(String symbol) {
    String upperSymbol = symbol.toUpperCase();
    Stock stock = stockCache.getIfPresent(upperSymbol);
    if (stock != null) {
      return stock;
    }

    try {
      stock = YahooFinance.get(upperSymbol, true);
      stockCache.put(upperSymbol, stock);
      return stock;
    } catch (IOException e) {
      return null;
    }
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
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      Stock stock = getStock(uppercaseSymbol);
      placeholderMap.put(uppercaseSymbol, new PlaceholderStock(stock, Utils.format(
          getServerPrice(stock), settings.getUnknownData(), settings.getLocale())));
      pendingOperations.remove(uppercaseSymbol);
    });

    return null;
  }

  private BigDecimal getFxRate(String fxSymbol) {
    String fxQuote = fxSymbol.toUpperCase() + "USD=X";
    FxQuote quote = fxCache.getIfPresent(fxQuote);
    if (quote != null) {
      return quote.getPrice();
    }

    try {
      quote = YahooFinance.getFx(fxQuote);
      fxCache.put(fxQuote, quote);
      return quote.getPrice();
    } catch (IOException e) {
      return null;
    }
  }

  private boolean isMarketOpen(String symbol) {
    Boolean result = marketOpenCache.getIfPresent(symbol.toUpperCase());
    if (result != null) {
      return result;
    }

    try {
      String requestXml = "<?xml version='1.0' encoding='utf−8'?><request devtype='Apple_OSX' " +
          "deployver='APPLE_DASHBOARD_1_0' app='YGoAppleStocksWidget' appver='unknown' " +
          "api='finance' apiver='1.0.1' acknotification='0000'><query id='0' timestamp='`" +
          "date +%s000`' type='getquotes'><list><symbol>" + symbol.toUpperCase() + "</symbol>"
          + "</list></query></request>";
      URL url = new URL("http://wu-quotes.apple.com/dgw?imei=42&apptype=finance");
      URLConnection con = url.openConnection();
      con.setDoInput(true);
      con.setDoOutput(true);
      con.setConnectTimeout(20_000);
      con.setReadTimeout(20_000);
      con.setUseCaches(false);
      con.setDefaultUseCaches(false);
      con.setRequestProperty("Content-Type", "text/xml");

      OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
      writer.write(requestXml);
      writer.flush();
      writer.close();

      InputStreamReader reader = new InputStreamReader(con.getInputStream());
      StringBuilder buf = new StringBuilder();
      char[] cbuf = new char[2048];
      int num;
      while (-1 != (num = reader.read(cbuf))) {
        buf.append(cbuf, 0, num);
      }

      reader.close();

      String finalResult = buf.toString();
      String[] initialSplit = finalResult.split("<status>");
      String[] finalSplit = initialSplit[1].split("</status>");

      result = finalSplit[0].equalsIgnoreCase("1");
    } catch (Throwable t) {
      result = false;
    }

    marketOpenCache.put(symbol.toUpperCase(), result);
    return result;
  }

  public BigDecimal getServerPrice(Stock stock) {
    BigDecimal price = stock.getQuote().getPrice().multiply(settings.getPriceMultiplier());
    if (!stock.getCurrency().equalsIgnoreCase("USD")) {
      BigDecimal conversionFactor = getFxRate(stock.getCurrency());
      if (conversionFactor == null) {
        return null;
      }

      price = price.multiply(conversionFactor);
    }

    return price;
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

    if (settings.isBlockTransactionsWhenClosed() && !isMarketOpen(stock.getSymbol())) {
      messages.sendMarketClosed(player);
      return true;
    }

    return false;
  }
}
