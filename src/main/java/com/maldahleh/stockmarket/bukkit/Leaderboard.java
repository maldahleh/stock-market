package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.market.Quantities;
import com.maldahleh.stockmarket.market.Quote;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.trade.TradeRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ranks players by the percent gain on what they hold now, so a small portfolio can beat a
 * large one. Players who have invested less than the minimum aren't ranked. Rebuilt on a timer
 * from one batched price request.
 */
public class Leaderboard {

  private final MarketData market;
  private final Portfolios portfolios;
  private final TradeRules rules;
  private final BigDecimal minimumInvested;
  private volatile List<Entry> ranking = List.of();

  public Leaderboard(MarketData market, Portfolios portfolios, TradeRules rules,
      BigDecimal minimumInvested) {
    this.market = market;
    this.portfolios = portfolios;
    this.rules = rules;
    this.minimumInvested = minimumInvested;
  }

  public List<Entry> ranking() {
    return ranking;
  }

  /** 1-based rank, or 0 if the player isn't ranked. */
  public int rankOf(UUID player) {
    List<Entry> current = ranking;
    for (int i = 0; i < current.size(); i++) {
      if (current.get(i).player().equals(player)) {
        return i + 1;
      }
    }
    return 0;
  }

  /** Fetches prices for every held symbol in one call and re-ranks. */
  public void refresh() {
    market.quotes(portfolios.heldSymbols()).thenAccept(this::rank).exceptionally(error -> null);
  }

  private void rank(Map<String, Quote> quotes) {
    List<Entry> entries = new ArrayList<>();
    for (UUID owner : portfolios.owners()) {
      BigDecimal paid = BigDecimal.ZERO;
      BigDecimal worth = BigDecimal.ZERO;
      boolean complete = true;
      for (Holding holding : portfolios.of(owner)) {
        Quote quote = quotes.get(holding.symbol());
        complete &= quote != null;
        if (quote != null) {
          paid = paid.add(holding.costBasis());
          worth = worth.add(Quantities.value(holding.symbol(),
              rules.serverPrice(quote.price()), holding.shares()));
        }
      }
      if (complete && paid.compareTo(minimumInvested) >= 0 && paid.signum() > 0) {
        entries.add(new Entry(owner, worth.subtract(paid)
            .divide(paid, 6, RoundingMode.HALF_UP).movePointRight(2)));
      }
    }
    entries.sort(Comparator.comparing(Entry::gainPercent).reversed());
    ranking = List.copyOf(entries);
  }

  /** One ranked player. */
  public record Entry(UUID player, BigDecimal gainPercent) {}
}
