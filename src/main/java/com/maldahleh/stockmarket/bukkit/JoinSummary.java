package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.market.Quantities;
import com.maldahleh.stockmarket.market.Quote;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.trade.TradeRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** One line on join: what the player's portfolio is worth and how it moved today. */
public class JoinSummary implements Listener {

  private final MarketData market;
  private final Portfolios portfolios;
  private final TradeRules rules;
  private final Messages messages;
  private final Formats formats;
  private final Executor serverThread;

  public JoinSummary(MarketData market, Portfolios portfolios, TradeRules rules,
      Messages messages, Formats formats, Executor serverThread) {
    this.market = market;
    this.portfolios = portfolios;
    this.rules = rules;
    this.messages = messages;
    this.formats = formats;
    this.serverThread = serverThread;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    UUID player = event.getPlayer().getUniqueId();
    List<Holding> holdings = portfolios.of(player);
    if (holdings.isEmpty() || messages.raw("portfolio-on-join").isBlank()) {
      return;
    }
    market.quotes(holdings.stream().map(Holding::symbol).toList())
        .thenAcceptAsync(quotes -> send(player, holdings, quotes), serverThread)
        .exceptionally(error -> null);
  }

  private void send(UUID owner, List<Holding> holdings, Map<String, Quote> quotes) {
    Player player = Bukkit.getPlayer(owner);
    if (player == null) {
      return;
    }
    BigDecimal worth = BigDecimal.ZERO;
    BigDecimal yesterday = BigDecimal.ZERO;
    BigDecimal paid = BigDecimal.ZERO;
    for (Holding holding : holdings) {
      Quote quote = quotes.get(holding.symbol());
      if (quote == null || quote.previousClose() == null) {
        return;
      }
      worth = worth.add(value(holding, quote.price()));
      yesterday = yesterday.add(value(holding, quote.previousClose()));
      paid = paid.add(holding.costBasis());
    }
    BigDecimal today = worth.subtract(yesterday);
    BigDecimal overall = worth.subtract(paid);
    messages.send(player, "portfolio-on-join", Map.of(
        "<value>", formats.money(worth),
        "<today>", formats.tint(today, formats.signedMoney(today)),
        "<today-percent>", formats.tint(today, formats.percent(percent(today, yesterday))),
        "<overall>", formats.tint(overall, formats.signedMoney(overall))));
  }

  private BigDecimal value(Holding holding, BigDecimal marketPrice) {
    return Quantities.value(holding.symbol(), rules.serverPrice(marketPrice), holding.shares());
  }

  private static BigDecimal percent(BigDecimal change, BigDecimal base) {
    return base.signum() == 0 ? null
        : change.divide(base, 8, RoundingMode.HALF_UP).movePointRight(2);
  }
}
