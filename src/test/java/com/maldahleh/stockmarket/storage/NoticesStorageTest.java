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
}
