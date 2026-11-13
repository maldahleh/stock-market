package com.maldahleh.stockmarket.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.maldahleh.stockmarket.FakeProvider;
import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.trade.TradeRules;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeaderboardTest {

  @Test
  void ranksByPercentGainAndSkipsSmallPortfolios() {
    FakeProvider provider = new FakeProvider();
    provider.price("AAPL", "110", Instant.now());
    provider.price("MSFT", "90", Instant.now());
    Portfolios portfolios = new Portfolios();
    UUID rich = UUID.randomUUID();
    UUID small = UUID.randomUUID();
    UUID loser = UUID.randomUUID();
    UUID tiny = UUID.randomUUID();
    portfolios.put(new Holding(rich, "AAPL", 100, new BigDecimal("10500")));
    portfolios.put(new Holding(small, "AAPL", 2, new BigDecimal("200")));
    portfolios.put(new Holding(loser, "MSFT", 10, new BigDecimal("1000")));
    portfolios.put(new Holding(tiny, "AAPL", 1, new BigDecimal("50")));
    TradeRules rules = new TradeRules(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, true,
        true, BigDecimal.ONE, Duration.ofMinutes(15), true, Set.of(), 1000);
    Leaderboard board = new Leaderboard(new MarketData(provider, Runnable::run,
        Duration.ofMinutes(1)), portfolios, rules, new BigDecimal("100"));

    board.refresh();

    // small: 200 → 220 is +10%; rich: 10500 → 11000 is +4.76%; loser: 1000 → 900 is -10%.
    assertEquals(3, board.ranking().size());
    assertEquals(small, board.ranking().get(0).player());
    assertEquals(rich, board.ranking().get(1).player());
    assertEquals(3, board.rankOf(loser));
    assertEquals(0, board.rankOf(tiny));
  }
}
