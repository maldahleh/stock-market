package com.maldahleh.stockmarket.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NoticesStorageTest {

  @TempDir
  File dir;

  @Test
  void noticesAreReturnedInOrderOnceThenDeleted() throws Exception {
    UUID player = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    try (Database database = Database.sqlite(new File(dir, "test.db"))) {
      database.addNotices(player, List.of("first", "second"));
      database.addNotices(other, List.of("someone else's"));
      database.addNotices(player, List.of("third"));
      assertEquals(List.of("first", "second", "third"), database.takeNotices(player));
      assertTrue(database.takeNotices(player).isEmpty());
      assertEquals(List.of("someone else's"), database.takeNotices(other));
    }
  }

  @Test
  void moneyTotalsGroupByType() throws Exception {
    UUID player = UUID.randomUUID();
    java.time.Instant now = java.time.Instant.now();
    try (Database database = Database.sqlite(new File(dir, "money.db"))) {
      database.buy(player, "AAPL", 2, new java.math.BigDecimal("210"),
          new com.maldahleh.stockmarket.portfolio.LedgerEntry(player,
              com.maldahleh.stockmarket.portfolio.EntryType.BUY, "AAPL", 2,
              new java.math.BigDecimal("100"), java.math.BigDecimal.TEN,
              new java.math.BigDecimal("-210"), now, null));
      var totals = database.moneySince(now.minusSeconds(60));
      var buys = totals.get(com.maldahleh.stockmarket.portfolio.EntryType.BUY);
      assertEquals(0, new java.math.BigDecimal("-210").compareTo(buys[0]));
      assertEquals(0, java.math.BigDecimal.TEN.compareTo(buys[1]));
      assertTrue(database.moneySince(now.plusSeconds(60)).isEmpty());
    }
  }
}
