package com.maldahleh.stockmarket.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.placeholder.model.PlaceholderStock;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.SchedulerUtils;
import java.math.BigDecimal;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import yahoofinance.Stock;

class StockPlaceholderManagerTests {

  private Plugin plugin;
  private StockManager stockManager;
  private Settings settings;

  @BeforeEach
  void setup() {
    this.plugin = mock(Plugin.class);
    this.stockManager = mock(StockManager.class);
    this.settings = mock(Settings.class);
  }

  @Test
  void getPlaceholderStock() {
    // GIVEN
    String symbol = "BA";

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      BukkitScheduler scheduler = mock(BukkitScheduler.class);

      bukkit.when(Bukkit::getScheduler)
          .thenReturn(scheduler);

      when(settings.getUnknownData())
          .thenReturn("N/A");

      when(settings.getLocale())
          .thenReturn(Locale.US);

      StockPlaceholderManager manager = new StockPlaceholderManager(plugin, stockManager, settings);
      // first call initiates the lookup
      PlaceholderStock firstCall = manager.getPlaceholderStock(symbol);
      // second call returns null but does not do a second lookup as first is pending for same
      // symbol
      PlaceholderStock secondCallWhileFirstExecuting = manager.getPlaceholderStock(symbol);

      Stock stock = mock(Stock.class);

      when(stockManager.getStock("BA"))
          .thenReturn(stock);

      when(stockManager.getServerPrice(stock))
          .thenReturn(BigDecimal.TEN);

      // allow the lookup to complete
      SchedulerUtils.interceptAsyncRun(plugin, scheduler);
      // run the timer from the constructor
      SchedulerUtils.interceptAsyncTimer(plugin, scheduler);

      // WHEN
      PlaceholderStock thirdCall = manager.getPlaceholderStock(symbol);

      // THEN
      assertNull(firstCall);
      assertNull(secondCallWhileFirstExecuting);

      assertNotNull(thirdCall);
      assertEquals("10.00", thirdCall.getServerPrice());
      assertEquals(stock, thirdCall.getStock());
    }
  }
}
