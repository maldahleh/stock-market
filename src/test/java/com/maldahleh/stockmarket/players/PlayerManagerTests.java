package com.maldahleh.stockmarket.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.players.listeners.PlayerListener;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.players.player.data.StockData;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import com.maldahleh.stockmarket.utils.SchedulerUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class PlayerManagerTests {

  private Plugin plugin;
  private StockManager stockManager;
  private Storage storage;
  private Settings settings;

  private PlayerManager playerManager;

  @BeforeEach
  void setup() {
    this.plugin = mock(Plugin.class);
    this.stockManager = mock(StockManager.class);
    this.storage = mock(Storage.class);
    this.settings = mock(Settings.class);

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager)
          .thenReturn(mock(PluginManager.class));

      this.playerManager = new PlayerManager(plugin, stockManager, storage, settings);
    }
  }

  @Test
  void registersListener() {
    // GIVEN
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      PluginManager pluginManager = mock(PluginManager.class);

      bukkit.when(Bukkit::getPluginManager)
          .thenReturn(pluginManager);

      // WHEN
      new PlayerManager(plugin, stockManager, storage, settings);

      // THEN
      verify(pluginManager)
          .registerEvents(any(PlayerListener.class), eq(plugin));
    }
  }

  @Test
  void cache() {
    // GIVEN
    UUID uuid = UUID.randomUUID();

    when(storage.getPlayerTransactions(uuid))
        .thenReturn(buildTransactionList());

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      BukkitScheduler scheduler = mock(BukkitScheduler.class);
      bukkit.when(Bukkit::getScheduler)
          .thenReturn(scheduler);

      // WHEN
      playerManager.cachePlayer(uuid);

      SchedulerUtils.interceptAsyncRun(plugin, scheduler);

      // THEN
      assertEquals(1, playerManager.getStockPlayer(uuid).getStockMap().get("BA").getQuantity());
      assertEquals(BigDecimal.valueOf(9), playerManager.getStockPlayer(uuid).getStockMap().get("BA")
          .getValue());

      verify(storage)
          .getPlayerTransactions(uuid);
    }
  }

  @Test
  void currentValue() {
    // GIVEN
    NavigableMap<String, StockData> dataMap = new TreeMap<>(Map.of(
        "BA", buildStockData()
    ));

    StockPlayer stockPlayer = mock(StockPlayer.class);
    when(stockPlayer.getStockMap())
        .thenReturn(dataMap);

    when(stockManager.getServerPrice("BA"))
        .thenReturn(BigDecimal.TEN);

    // WHEN
    BigDecimal currentValue = playerManager.getCurrentValue(stockPlayer);

    // THEN
    assertEquals(BigDecimal.TEN, currentValue);
  }

  @Nested
  @DisplayName("forceGetStockPlayer")
  class ForceGetStockPlayer {

    @Test
    void notCached() {
      // GIVEN
      UUID uuid = UUID.randomUUID();

      when(storage.getPlayerTransactions(uuid))
          .thenReturn(buildTransactionList());

      // WHEN
      StockPlayer player = playerManager.forceGetStockPlayer(uuid);

      // THEN
      assertEquals(1, player.getStockMap().get("BA").getQuantity());
      assertEquals(BigDecimal.valueOf(9), player.getStockMap().get("BA").getValue());

      // player is uncached after force get
      assertNull(playerManager.getStockPlayer(uuid));

      verify(storage)
          .getPlayerTransactions(uuid);
    }

    @Test
    void cached() {
      // GIVEN
      UUID uuid = UUID.randomUUID();

      when(storage.getPlayerTransactions(uuid))
          .thenReturn(buildTransactionList());

      try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        bukkit.when(Bukkit::getScheduler)
            .thenReturn(scheduler);

        playerManager.cachePlayer(uuid);
        SchedulerUtils.interceptAsyncRun(plugin, scheduler);

        // WHEN
        StockPlayer stockPlayer = playerManager.forceGetStockPlayer(uuid);

        // THEN
        assertEquals(1, stockPlayer.getStockMap().get("BA").getQuantity());
        assertEquals(BigDecimal.valueOf(9), stockPlayer.getStockMap().get("BA").getValue());

        // only called once (during initial cache)
        verify(storage)
            .getPlayerTransactions(uuid);
      }
    }
  }

  @Nested
  @DisplayName("canNotPerformTransaction")
  class CanNotPerformTransaction {

    @Test
    void zeroSecondCooldown() {
      // GIVEN
      UUID uuid = UUID.randomUUID();
      int cooldown = 0;

      when(settings.getTransactionCooldownSeconds())
          .thenReturn(cooldown);

      // WHEN
      boolean canNotPerform = playerManager.canNotPerformTransaction(uuid);

      // THEN
      assertFalse(canNotPerform);
    }

    @Nested
    class ThirtySecondCooldown {

      private static final int COOLDOWN_SECONDS = 30;

      @BeforeEach
      void setup() {
        when(settings.getTransactionCooldownSeconds())
            .thenReturn(COOLDOWN_SECONDS);
      }

      @Test
      void noPreviousAction() {
        // GIVEN
        UUID uuid = UUID.randomUUID();

        // WHEN
        boolean canNotPerform = playerManager.canNotPerformTransaction(uuid);

        // THEN
        assertFalse(canNotPerform);
      }

      @Test
      void cooldownNotElapsed() {
        // GIVEN
        UUID uuid = UUID.randomUUID();

        when(storage.getPlayerTransactions(uuid))
            .thenReturn(buildTransactionList());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
          BukkitScheduler scheduler = mock(BukkitScheduler.class);
          bukkit.when(Bukkit::getScheduler)
              .thenReturn(scheduler);

          playerManager.cachePlayer(uuid);
          SchedulerUtils.interceptAsyncRun(plugin, scheduler);

          // WHEN
          boolean canNotPerform = playerManager.canNotPerformTransaction(uuid);

          // THEN
          assertTrue(canNotPerform);
        }
      }
    }
  }

  private StockData buildStockData() {
    List<Transaction> transactions = buildTransactionList();

    StockData stockData = new StockData();
    stockData.increase(transactions.get(0));
    stockData.decrease(transactions.get(1));

    return stockData;
  }

  private List<Transaction> buildTransactionList() {
    return List.of(
        Transaction.builder()
            .symbol("BA")
            .type(TransactionType.PURCHASE)
            .quantity(3)
            .stockValue(BigDecimal.TEN)
            .build(),
        Transaction.builder()
            .symbol("BA")
            .type(TransactionType.SALE)
            .quantity(2)
            .stockValue(BigDecimal.ONE)
            .build()
    );
  }
}
