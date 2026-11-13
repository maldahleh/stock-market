package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.market.Asset;
import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.market.Quantities;
import com.maldahleh.stockmarket.market.Quote;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.trade.TradeRules;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * %sm_portfolio-value% and %sm_sd-SYMBOL-POINT% (points: name, sp, price, vol, change). Never
 * blocks: unknown values show as N/A while a background fetch fills the cache.
 */
public class StockExpansion extends PlaceholderExpansion {

  private static final Duration REFRESH = Duration.ofSeconds(30);

  private final MarketData market;
  private final Portfolios portfolios;
  private final TradeRules rules;
  private final Formats formats;
  private final String version;
  private final Leaderboard leaderboard;
  private final Map<String, Instant> requested = new ConcurrentHashMap<>();

  public StockExpansion(MarketData market, Portfolios portfolios, TradeRules rules,
      Formats formats, String version, Leaderboard leaderboard) {
    this.leaderboard = leaderboard;
    this.market = market;
    this.portfolios = portfolios;
    this.rules = rules;
    this.formats = formats;
    this.version = version;
  }

  @Override
  public String getIdentifier() {
    return "sm";
  }

  @Override
  public String getAuthor() {
    return "maldahleh";
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public String onRequest(OfflinePlayer player, String params) {
    if (params.equalsIgnoreCase("portfolio-value")) {
      return player == null ? formats.unknown() : portfolioValue(player);
    }
    if (params.equalsIgnoreCase("rank")) {
      int rank = player == null ? 0 : leaderboard.rankOf(player.getUniqueId());
      return rank == 0 ? "-" : String.valueOf(rank);
    }
    if (params.toLowerCase().startsWith("top-")) {
      return top(params.toLowerCase());
    }
    return params.toLowerCase().startsWith("sd-") ? stockData(params) : null;
  }

  /** %sm_sd-SYMBOL-POINT%. */
  private String stockData(String params) {
    int split = params.lastIndexOf('-');
    if (split <= 3) {
      return null;
    }
    String symbol = params.substring(3, split).toUpperCase();
    String point = params.substring(split + 1).toLowerCase();

    Optional<Quote> quote = quote(symbol);
    return switch (point) {
      case "name" -> market.cachedAsset(symbol).map(Asset::name).orElse(formats.unknown());
      case "sp" -> quote.map(q -> formats.money(rules.serverPrice(q.price())))
          .orElse(formats.unknown());
      case "price" -> quote.map(q -> formats.usd(q.price())).orElse(formats.unknown());
      case "vol" -> quote.map(q -> formats.compact(q.volume())).orElse(formats.unknown());
      case "change" -> quote.map(q -> formats.percent(q.changeFromClose()))
          .orElse(formats.unknown());
      default -> null;
    };
  }

  /** %sm_top-1-name% or %sm_top-1-gain%. */
  private String top(String params) {
    String[] parts = params.split("-");
    if (parts.length != 3) {
      return null;
    }
    int place;
    try {
      place = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      return null;
    }
    List<Leaderboard.Entry> ranking = leaderboard.ranking();
    if (place < 1 || place > ranking.size()) {
      return "-";
    }
    Leaderboard.Entry entry = ranking.get(place - 1);
    return switch (parts[2]) {
      case "name" -> {
        String name = org.bukkit.Bukkit.getOfflinePlayer(entry.player()).getName();
        yield name == null ? "-" : name;
      }
      case "gain" -> formats.percent(entry.gainPercent());
      default -> null;
    };
  }

  private String portfolioValue(OfflinePlayer player) {
    BigDecimal total = BigDecimal.ZERO;
    for (Holding holding : portfolios.of(player.getUniqueId())) {
      Optional<Quote> quote = quote(holding.symbol());
      if (quote.isEmpty()) {
        return formats.unknown();
      }
      total = total.add(Quantities.value(holding.symbol(),
          rules.serverPrice(quote.get().price()), holding.shares()));
    }
    return formats.money(total);
  }

  /** The cached quote; also starts a background refresh at most every 30 seconds per symbol. */
  private Optional<Quote> quote(String symbol) {
    Instant now = Instant.now();
    Instant last = requested.get(symbol);
    if (last == null || last.plus(REFRESH).isBefore(now)) {
      requested.put(symbol, now);
      market.quotes(List.of(symbol)).exceptionally(e -> Map.of());
      if (market.cachedAsset(symbol).isEmpty()) {
        market.asset(symbol).exceptionally(e -> Optional.empty());
      }
    }
    return market.cachedQuote(symbol);
  }
}
