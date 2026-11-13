package com.maldahleh.stockmarket.market.alpaca;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maldahleh.stockmarket.actions.CorporateAction;
import com.maldahleh.stockmarket.market.Asset;
import com.maldahleh.stockmarket.market.AssetClass;
import com.maldahleh.stockmarket.market.DailyBar;
import com.maldahleh.stockmarket.market.MarketClock;
import com.maldahleh.stockmarket.market.MarketDataException;
import com.maldahleh.stockmarket.market.MarketDataProvider;
import com.maldahleh.stockmarket.market.Quote;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Market data from Alpaca. A free paper-trading account is enough: the free plan gives real-time
 * prices from the IEX exchange, crypto, daily bars, the market clock and corporate actions.
 */
public class AlpacaProvider implements MarketDataProvider {

  static final String DATA_HOST = "https://data.alpaca.markets";
  static final String PAPER_HOST = "https://paper-api.alpaca.markets";
  static final String LIVE_HOST = "https://api.alpaca.markets";

  private static final int SYMBOLS_PER_REQUEST = 100;
  private static final String ACTION_TYPES = String.join(",",
      "forward_split", "reverse_split", "stock_dividend", "name_change", "cash_merger",
      "stock_merger", "stock_and_cash_merger", "spin_off", "worthless_removal", "redemption", "cash_dividend");

  private final HttpClient http;
  private final String keyId;
  private final String secret;
  private final String feed;
  private final String tradingHost;
  private final String dataHost;

  public AlpacaProvider(String keyId, String secret, String feed, boolean liveAccount) {
    this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), keyId, secret,
        feed, liveAccount ? LIVE_HOST : PAPER_HOST, DATA_HOST);
  }

  AlpacaProvider(HttpClient http, String keyId, String secret, String feed, String tradingHost,
      String dataHost) {
    this.http = http;
    this.keyId = keyId;
    this.secret = secret;
    this.feed = feed;
    this.tradingHost = tradingHost;
    this.dataHost = dataHost;
  }

  @Override
  public Map<String, Quote> quotes(Collection<String> symbols) throws MarketDataException {
    List<String> stocks = new ArrayList<>();
    List<String> crypto = new ArrayList<>();
    for (String symbol : symbols) {
      (AssetClass.of(symbol) == AssetClass.CRYPTO ? crypto : stocks).add(symbol);
    }

    Map<String, Quote> quotes = new HashMap<>();
    for (List<String> chunk : chunks(stocks)) {
      JsonObject body = get(dataHost + "/v2/stocks/snapshots?feed=" + feed + "&symbols="
          + encode(String.join(",", chunk)));
      quotes.putAll(AlpacaJson.parseSnapshots(body, AssetClass.STOCK));
    }
    for (List<String> chunk : chunks(crypto)) {
      JsonObject body = get(dataHost + "/v1beta3/crypto/us/snapshots?symbols="
          + encode(String.join(",", chunk)));
      quotes.putAll(AlpacaJson.parseSnapshots(body.getAsJsonObject("snapshots"),
          AssetClass.CRYPTO));
    }
    return quotes;
  }

  @Override
  public Optional<Asset> asset(String symbol) throws MarketDataException {
    try {
      return Optional.of(AlpacaJson.parseAsset(
          get(tradingHost + "/v2/assets/" + encode(symbol))));
    } catch (MarketDataException e) {
      if (e.status() == 404 || e.status() == 422) {
        return Optional.empty();
      }
      throw e;
    }
  }

  @Override
  public List<DailyBar> dailyBars(String symbol, int days) throws MarketDataException {
    // Calendar days cover weekends and holidays; the limit trims to trading days.
    String start = LocalDate.now().minusDays(days * 3L / 2 + 10).toString();
    String query = "?timeframe=1Day&sort=desc&limit=" + days + "&start=" + start
        + "&symbols=" + encode(symbol);
    JsonObject body = AssetClass.of(symbol) == AssetClass.CRYPTO
        ? get(dataHost + "/v1beta3/crypto/us/bars" + query)
        : get(dataHost + "/v2/stocks/bars" + query + "&adjustment=split&feed=" + feed);

    List<DailyBar> bars = new ArrayList<>(AlpacaJson.parseBars(body, symbol));
    Collections.reverse(bars);
    return bars;
  }

  @Override
  public MarketClock clock() throws MarketDataException {
    return AlpacaJson.parseClock(get(tradingHost + "/v2/clock"), Instant.now());
  }

  @Override
  public List<CorporateAction> corporateActions(Collection<String> symbols, LocalDate from,
      LocalDate to) throws MarketDataException {
    List<CorporateAction> actions = new ArrayList<>();
    for (List<String> chunk : chunks(symbols.stream()
        .filter(s -> AssetClass.of(s) == AssetClass.STOCK).toList())) {
      String pageToken = null;
      do {
        String url = dataHost + "/v1/corporate-actions?limit=1000&types=" + ACTION_TYPES
            + "&start=" + from + "&end=" + to + "&symbols=" + encode(String.join(",", chunk))
            + (pageToken == null ? "" : "&page_token=" + encode(pageToken));
        JsonObject body = get(url);
        actions.addAll(AlpacaJson.parseCorporateActions(body));
        JsonElement next = body.get("next_page_token");
        pageToken = next == null || next.isJsonNull() ? null : next.getAsString();
      } while (pageToken != null);
    }
    return actions;
  }

  private JsonObject get(String url) throws MarketDataException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(15))
        .header("APCA-API-KEY-ID", keyId)
        .header("APCA-API-SECRET-KEY", secret)
        .header("Accept", "application/json")
        .GET()
        .build();

    HttpResponse<String> response;
    try {
      response = http.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new MarketDataException("Could not reach Alpaca: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MarketDataException("Interrupted while calling Alpaca", e);
    }

    if (response.statusCode() != 200) {
      throw new MarketDataException(describe(response), response.statusCode());
    }
    try {
      JsonElement element = JsonParser.parseString(response.body());
      return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    } catch (RuntimeException e) {
      throw new MarketDataException("Alpaca sent a response that isn't JSON", e);
    }
  }

  private static String describe(HttpResponse<String> response) {
    String path = response.uri().getPath();
    return switch (response.statusCode()) {
      case 401 -> "Alpaca rejected the API key (401). Check alpaca.key-id and alpaca.secret.";
      case 403 -> "Alpaca says this key's plan can't use " + path + " (403).";
      case 429 -> "Alpaca rate limit reached (429).";
      default -> "Alpaca returned " + response.statusCode() + " for " + path + ": "
          + truncate(response.body());
    };
  }

  private static String truncate(String body) {
    return body == null ? "" : body.length() > 200 ? body.substring(0, 200) + "..." : body;
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static List<List<String>> chunks(Collection<String> items) {
    List<String> unique = new ArrayList<>(new LinkedHashSet<>(items));
    List<List<String>> chunks = new ArrayList<>();
    for (int i = 0; i < unique.size(); i += SYMBOLS_PER_REQUEST) {
      chunks.add(unique.subList(i, Math.min(unique.size(), i + SYMBOLS_PER_REQUEST)));
    }
    return chunks;
  }

  static JsonArray array(JsonObject object, String key) {
    JsonElement element = object == null ? null : object.get(key);
    return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
  }

  static BigDecimal decimal(JsonObject object, String key) {
    JsonElement element = object == null ? null : object.get(key);
    if (element == null || element.isJsonNull()) {
      return null;
    }
    try {
      return element.getAsBigDecimal();
    } catch (RuntimeException e) {
      return null;
    }
  }
}
