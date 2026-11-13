package com.maldahleh.stockmarket.menu;

import com.maldahleh.stockmarket.bukkit.Formats;
import com.maldahleh.stockmarket.market.Asset;
import com.maldahleh.stockmarket.market.DailyBar;
import com.maldahleh.stockmarket.market.Quote;
import com.maldahleh.stockmarket.trade.TradeRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Builds the placeholders that describe one stock in menus. */
public final class StockView {

  private StockView() {}

  public static Map<String, String> placeholders(String symbol, Asset asset, Quote quote,
      List<DailyBar> bars, long owned, TradeRules rules, Formats f) {
    Map<String, String> p = new HashMap<>();
    p.put("<symbol>", symbol);
    p.put("<market-currency>", "USD");
    p.put("<owned>", f.quantity(symbol, owned));
    p.put("<broker-flat>", f.money(rules.flatFee()));
    p.put("<broker-percent>", f.plain(rules.percentFee()));
    putAsset(p, symbol, asset, f);
    putQuote(p, quote, rules, f);
    putYear(p, quote == null ? null : quote.price(), bars == null ? List.of() : bars, f);
    return p;
  }

  private static void putAsset(Map<String, String> p, String symbol, Asset asset, Formats f) {
    String name = asset == null ? null : asset.name();
    String exchange = asset == null ? null : asset.exchange();
    p.put("<name>", name == null ? symbol : name);
    p.put("<exchange>", exchange == null ? f.unknown() : exchange);
  }

  private static void putQuote(Map<String, String> p, Quote quote, TradeRules rules,
      Formats f) {
    if (quote == null) {
      for (String key : List.of("<market-price>", "<server-price>", "<change-close>",
          "<open-price>", "<day-high>", "<day-low>", "<close-price>", "<volume>",
          "<last-trade>")) {
        p.put(key, f.unknown());
      }
      return;
    }
    p.put("<market-price>", f.usd(quote.price()));
    p.put("<server-price>", f.money(rules.serverPrice(quote.price())));
    p.put("<change-close>", f.tint(quote.changeFromClose(), f.percent(quote.changeFromClose())));
    p.put("<open-price>", f.usd(quote.open()));
    p.put("<day-high>", f.usd(quote.high()));
    p.put("<day-low>", f.usd(quote.low()));
    p.put("<close-price>", f.usd(quote.previousClose()));
    p.put("<volume>", f.compact(quote.volume()));
    p.put("<last-trade>", f.date(quote.tradeTime()));
  }

  private static void putYear(Map<String, String> p, BigDecimal price, List<DailyBar> year,
      Formats f) {
    BigDecimal yearHigh = year.stream().map(DailyBar::high).filter(Objects::nonNull)
        .max(Comparator.naturalOrder()).orElse(null);
    BigDecimal yearLow = year.stream().map(DailyBar::low).filter(Objects::nonNull)
        .min(Comparator.naturalOrder()).orElse(null);
    p.put("<year-high>", f.usd(yearHigh));
    p.put("<year-low>", f.usd(yearLow));
    putChange(p, "<change-year-high>", change(price, yearHigh), f);
    putChange(p, "<change-year-low>", change(price, yearLow), f);
    putChange(p, "<change-50-moving-avg>", change(price, average(year, 50)), f);
    putChange(p, "<change-200-moving-avg>", change(price, average(year, 200)), f);
  }

  private static void putChange(Map<String, String> p, String key, BigDecimal change,
      Formats f) {
    p.put(key, f.tint(change, f.percent(change)));
  }

  public static Map<String, String> bar(DailyBar bar, Formats f) {
    return Map.of(
        "<date>", bar.date().toString(),
        "<market-currency>", "USD",
        "<day-open>", f.usd(bar.open()),
        "<day-close>", f.usd(bar.close()),
        "<day-high>", f.usd(bar.high()),
        "<day-low>", f.usd(bar.low()),
        "<volume>", f.compact(bar.volume()));
  }

  /** Average close of the last {@code days} bars, or null if there aren't that many. */
  static BigDecimal average(List<DailyBar> bars, int days) {
    if (bars.size() < days) {
      return null;
    }
    BigDecimal sum = BigDecimal.ZERO;
    for (DailyBar bar : bars.subList(bars.size() - days, bars.size())) {
      if (bar.close() == null) {
        return null;
      }
      sum = sum.add(bar.close());
    }
    return sum.divide(BigDecimal.valueOf(days), 6, RoundingMode.HALF_UP);
  }

  /** Percent change from {@code base} to {@code price}. */
  static BigDecimal change(BigDecimal price, BigDecimal base) {
    if (price == null || base == null || base.signum() == 0) {
      return null;
    }
    return price.subtract(base).divide(base, 8, RoundingMode.HALF_UP).movePointRight(2);
  }
}
