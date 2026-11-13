package com.maldahleh.stockmarket.market.alpaca;

import static com.maldahleh.stockmarket.market.alpaca.AlpacaProvider.array;
import static com.maldahleh.stockmarket.market.alpaca.AlpacaProvider.decimal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.maldahleh.stockmarket.actions.CorporateAction;
import com.maldahleh.stockmarket.actions.CorporateAction.CashDividend;
import com.maldahleh.stockmarket.actions.CorporateAction.Merger;
import com.maldahleh.stockmarket.actions.CorporateAction.NameChange;
import com.maldahleh.stockmarket.actions.CorporateAction.Removal;
import com.maldahleh.stockmarket.actions.CorporateAction.SpinOff;
import com.maldahleh.stockmarket.actions.CorporateAction.Split;
import com.maldahleh.stockmarket.market.Asset;
import com.maldahleh.stockmarket.market.AssetClass;
import com.maldahleh.stockmarket.market.DailyBar;
import com.maldahleh.stockmarket.market.MarketClock;
import com.maldahleh.stockmarket.market.Quote;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Turns Alpaca's JSON into the plugin's types. Package-private so tests can feed it fixtures. */
final class AlpacaJson {

  private AlpacaJson() {}

  static Map<String, Quote> parseSnapshots(JsonObject snapshots, AssetClass assetClass) {
    Map<String, Quote> quotes = new HashMap<>();
    if (snapshots == null) {
      return quotes;
    }
    for (Map.Entry<String, JsonElement> entry : snapshots.entrySet()) {
      if (!entry.getValue().isJsonObject()) {
        continue;
      }
      JsonObject snapshot = entry.getValue().getAsJsonObject();
      JsonObject trade = object(snapshot, "latestTrade");
      BigDecimal price = decimal(trade, "p");
      Instant time = instant(trade, "t");
      if (price == null || price.signum() <= 0 || time == null) {
        continue;
      }
      JsonObject daily = object(snapshot, "dailyBar");
      JsonObject previous = object(snapshot, "prevDailyBar");
      BigDecimal volume = decimal(daily, "v");
      String symbol = entry.getKey().toUpperCase();
      quotes.put(symbol, new Quote(symbol, assetClass, price, time, decimal(daily, "o"),
          decimal(daily, "h"), decimal(daily, "l"), decimal(previous, "c"),
          volume == null ? 0 : volume.longValue()));
    }
    return quotes;
  }

  static Asset parseAsset(JsonObject asset) {
    boolean active = "active".equalsIgnoreCase(string(asset, "status"));
    boolean tradable = asset.has("tradable") && asset.get("tradable").getAsBoolean();
    return new Asset(string(asset, "symbol"), string(asset, "name"), string(asset, "exchange"),
        active && tradable);
  }

  static List<DailyBar> parseBars(JsonObject body, String symbol) {
    JsonObject bars = object(body, "bars");
    List<DailyBar> result = new ArrayList<>();
    if (bars == null) {
      return result;
    }
    for (JsonElement element : array(bars, symbol)) {
      JsonObject bar = element.getAsJsonObject();
      Instant time = instant(bar, "t");
      BigDecimal volume = decimal(bar, "v");
      if (time == null) {
        continue;
      }
      result.add(new DailyBar(LocalDate.ofInstant(time, java.time.ZoneOffset.UTC),
          decimal(bar, "o"), decimal(bar, "h"), decimal(bar, "l"), decimal(bar, "c"),
          volume == null ? 0 : volume.longValue()));
    }
    return result;
  }

  static MarketClock parseClock(JsonObject clock, Instant now) {
    return new MarketClock(clock.has("is_open") && clock.get("is_open").getAsBoolean(),
        instant(clock, "next_open"), instant(clock, "next_close"), now);
  }

  static List<CorporateAction> parseCorporateActions(JsonObject body) {
    JsonObject groups = object(body, "corporate_actions");
    List<CorporateAction> actions = new ArrayList<>();
    if (groups == null) {
      return actions;
    }

    for (JsonObject a : objects(groups, "forward_splits")) {
      actions.add(new Split(string(a, "id"), date(a, "ex_date"), string(a, "symbol"), null,
          decimal(a, "old_rate"), decimal(a, "new_rate")));
    }
    for (JsonObject a : objects(groups, "reverse_splits")) {
      actions.add(new Split(string(a, "id"), date(a, "ex_date"), string(a, "symbol"),
          string(a, "new_symbol"), decimal(a, "old_rate"), decimal(a, "new_rate")));
    }
    for (JsonObject a : objects(groups, "stock_dividends")) {
      BigDecimal rate = decimal(a, "rate");
      if (rate != null) {
        actions.add(new Split(string(a, "id"), date(a, "ex_date"), string(a, "symbol"), null,
            BigDecimal.ONE, BigDecimal.ONE.add(rate)));
      }
    }
    for (JsonObject a : objects(groups, "name_changes")) {
      actions.add(new NameChange(string(a, "id"), date(a, "process_date"),
          string(a, "old_symbol"), string(a, "new_symbol")));
    }
    for (JsonObject a : objects(groups, "cash_mergers")) {
      actions.add(new Merger(string(a, "id"), date(a, "effective_date"),
          string(a, "acquiree_symbol"), null, null, null, decimal(a, "rate")));
    }
    for (JsonObject a : objects(groups, "stock_mergers")) {
      actions.add(new Merger(string(a, "id"), date(a, "effective_date"),
          string(a, "acquiree_symbol"), string(a, "acquirer_symbol"),
          decimal(a, "acquiree_rate"), decimal(a, "acquirer_rate"), BigDecimal.ZERO));
    }
    for (JsonObject a : objects(groups, "stock_and_cash_mergers")) {
      actions.add(new Merger(string(a, "id"), date(a, "effective_date"),
          string(a, "acquiree_symbol"), string(a, "acquirer_symbol"),
          decimal(a, "acquiree_rate"), decimal(a, "acquirer_rate"), decimal(a, "cash_rate")));
    }
    for (JsonObject a : objects(groups, "redemptions")) {
      actions.add(new Merger(string(a, "id"), date(a, "process_date"), string(a, "symbol"),
          null, null, null, decimal(a, "rate")));
    }
    for (JsonObject a : objects(groups, "spin_offs")) {
      actions.add(new SpinOff(string(a, "id"), date(a, "ex_date"), string(a, "source_symbol"),
          string(a, "new_symbol"), decimal(a, "source_rate"), decimal(a, "new_rate")));
    }
    for (JsonObject a : objects(groups, "cash_dividends")) {
      String currency = string(a, "currency");
      if (currency != null && !currency.isBlank() && !currency.equalsIgnoreCase("USD")) {
        continue;
      }
      LocalDate payable = date(a, "payable_date");
      actions.add(new CashDividend(string(a, "id"), date(a, "ex_date"),
          payable == null ? date(a, "process_date") : payable, string(a, "symbol"),
          decimal(a, "rate")));
    }
    for (JsonObject a : objects(groups, "worthless_removals")) {
      actions.add(new Removal(string(a, "id"), date(a, "process_date"), string(a, "symbol")));
    }

    actions.removeIf(AlpacaJson::incomplete);
    return actions;
  }

  /** Drops records missing the fields their math needs, rather than guessing. */
  private static boolean incomplete(CorporateAction action) {
    if (action.id() == null || action.effectiveDate() == null
        || action.sourceSymbols().stream().anyMatch(s -> s == null || s.isBlank())) {
      return true;
    }
    return switch (action) {
      case Split s -> !positive(s.oldRate()) || !positive(s.newRate());
      case NameChange n -> n.newSymbol() == null || n.newSymbol().isBlank();
      case Merger m -> m.hasStock() ? !positive(m.acquireeRate())
          : m.cashPerShare() == null || m.cashPerShare().signum() < 0;
      case SpinOff s -> s.newSymbol() == null || !positive(s.sourceRate())
          || !positive(s.newRate());
      case Removal r -> false;
      case CashDividend d -> !positive(d.rate());
    };
  }

  private static boolean positive(BigDecimal value) {
    return value != null && value.signum() > 0;
  }

  private static List<JsonObject> objects(JsonObject parent, String key) {
    List<JsonObject> objects = new ArrayList<>();
    for (JsonElement element : array(parent, key)) {
      if (element.isJsonObject()) {
        objects.add(element.getAsJsonObject());
      }
    }
    return objects;
  }

  private static JsonObject object(JsonObject parent, String key) {
    JsonElement element = parent == null ? null : parent.get(key);
    return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
  }

  private static String string(JsonObject object, String key) {
    JsonElement element = object == null ? null : object.get(key);
    return element == null || element.isJsonNull() ? null : element.getAsString();
  }

  private static Instant instant(JsonObject object, String key) {
    String value = string(object, key);
    try {
      return value == null ? null : OffsetDateTime.parse(value).toInstant();
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static LocalDate date(JsonObject object, String key) {
    String value = string(object, key);
    try {
      return value == null ? null : LocalDate.parse(value);
    } catch (RuntimeException e) {
      return null;
    }
  }
}
