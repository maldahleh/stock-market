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
    p.put("<name>", asset == null || asset.name() == null ? symbol : asset.name());
    p.put("<exchange>", asset == null || asset.exchange() == null ? f.unknown()
        : asset.exchange());
    p.put("<market-currency>", "USD");
    p.put("<owned>", f.number(owned));
    p.put("<broker-flat>", f.money(rules.flatFee()));
    p.put("<broker-percent>", f.plain(rules.percentFee()));

    BigDecimal price = quote == null ? null : quote.price();
    p.put("<market-price>", f.usd(price));
    p.put("<server-price>", price == null ? f.unknown() : f.money(rules.serverPrice(price)));
    p.put("<change-close>", f.percent(quote == null ? null : quote.changeFromClose()));
    p.put("<open-price>", f.usd(quote == null ? null : quote.open()));
    p.put("<day-high>", f.usd(quote == null ? null : quote.high()));
    p.put("<day-low>", f.usd(quote == null ? null : quote.low()));
    p.put("<close-price>", f.usd(quote == null ? null : quote.previousClose()));
    p.put("<volume>", quote == null ? f.unknown() : f.compact(quote.volume()));
    p.put("<last-trade>", quote == null ? f.unknown() : f.date(quote.tradeTime()));

    List<DailyBar> year = bars == null ? List.of() : bars;
    BigDecimal yearHigh = year.stream().map(DailyBar::high).filter(Objects::nonNull)
        .max(Comparator.naturalOrder()).orElse(null);
    BigDecimal yearLow = year.stream().map(DailyBar::low).filter(Objects::nonNull)
        .min(Comparator.naturalOrder()).orElse(null);
    p.put("<year-high>", f.usd(yearHigh));
    p.put("<year-low>", f.usd(yearLow));
    p.put("<change-year-high>", f.percent(change(price, yearHigh)));
    p.put("<change-year-low>", f.percent(change(price, yearLow)));
    p.put("<change-50-moving-avg>", f.percent(change(price, average(year, 50))));
    p.put("<change-200-moving-avg>", f.percent(change(price, average(year, 200))));
    return p;
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
