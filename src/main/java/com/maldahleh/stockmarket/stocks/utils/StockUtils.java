package com.maldahleh.stockmarket.stocks.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import org.bukkit.configuration.ConfigurationSection;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.fx.FxQuote;

@UtilityClass
public class StockUtils {

  public Stock fetchStock(String symbol) {
    try {
      return YahooFinance.get(symbol, true);
    } catch (IOException e) {
      return null;
    }
  }

  public Map<String, Stock> fetchStocks(String... symbols) {
    Map<String, Stock> stocks = lookupStocks(symbols);
    stocks.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);

    return stocks;
  }

  private Map<String, Stock> lookupStocks(String... symbols) {
    try {
      return YahooFinance.get(symbols);
    } catch (IOException e) {
      return new HashMap<>();
    }
  }

  public FxQuote fetchFxQuote(String fxQuote) {
    try {
      return YahooFinance.getFx(fxQuote);
    } catch (IOException e) {
      return null;
    }
  }

  public boolean isMarketOpen(String symbol) {
    try {
      String requestXml =
          "<?xml version='1.0' encoding='utf−8'?><request devtype='Apple_OSX' "
              + "deployver='APPLE_DASHBOARD_1_0' app='YGoAppleStocksWidget' appver='unknown' "
              + "api='finance' apiver='1.0.1' acknotification='0000'><query id='0' timestamp='`"
              + "date +%s000`' type='getquotes'><list><symbol>"
              + symbol.toUpperCase()
              + "</symbol>"
              + "</list></query></request>";
      URL url = new URL("https://wu-quotes.apple.com/dgw?imei=42&apptype=finance");
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

      return finalSplit[0].equalsIgnoreCase("1");
    } catch (Exception e) {
      return false;
    }
  }

  public <T> Cache<String, T> buildCache(ConfigurationSection section) {
    return CacheBuilder.newBuilder()
        .expireAfterWrite(section.getInt("cache.expire-minutes"), TimeUnit.MINUTES)
        .maximumSize(500)
        .build();
  }
}
