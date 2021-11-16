package com.maldahleh.stockmarket.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.placeholder.model.PlaceholderStock;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import yahoofinance.Stock;
import yahoofinance.quotes.stock.StockQuote;
import yahoofinance.quotes.stock.StockStats;

class StockPlaceholderTests {

  private Settings settings;
  private PlayerManager playerManager;
  private StockPlaceholderManager stockPlaceholderManager;

  private StockPlaceholder stockPlaceholder;

  @BeforeEach
  void setup() {
    this.settings = mock(Settings.class);
    this.playerManager = mock(PlayerManager.class);
    this.stockPlaceholderManager = mock(StockPlaceholderManager.class);

    this.stockPlaceholder = new StockPlaceholder(settings, playerManager, stockPlaceholderManager);
  }

  @Test
  void properties() {
    assertEquals("sm", stockPlaceholder.getIdentifier());
    assertEquals("maldahleh", stockPlaceholder.getAuthor());
    assertEquals("1.0", stockPlaceholder.getVersion());
    assertTrue(stockPlaceholder.persist());
  }

  @Test
  void invalidParams() {
    // GIVEN
    String params = "unknown";
    OfflinePlayer player = mock(OfflinePlayer.class);

    // WHEN
    String result = stockPlaceholder.onRequest(player, params);

    // THEN
    assertNull(result);
  }

  @Nested
  class PortfolioValue {

    @Test
    void nullPlayer() {
      // GIVEN
      String params = "portfolio-value";

      // WHEN
      String result = stockPlaceholder.onRequest(null, params);

      // THEN
      assertEquals("Player Offline", result);
    }

    @Test
    void offlinePlayer() {
      // GIVEN
      String params = "portfolio-value";
      OfflinePlayer player = mock(OfflinePlayer.class);

      when(player.isOnline())
          .thenReturn(false);

      // WHEN
      String result = stockPlaceholder.onRequest(player, params);

      // THEN
      assertEquals("Player Offline", result);
    }

    @Test
    void notStockPlayer() {
      // GIVEN
      String params = "portfolio-value";
      UUID uuid = UUID.randomUUID();
      OfflinePlayer player = mock(OfflinePlayer.class);

      when(player.isOnline())
          .thenReturn(true);

      when(player.getUniqueId())
          .thenReturn(uuid);

      when(playerManager.getStockPlayer(uuid))
          .thenReturn(null);

      // WHEN
      String result = stockPlaceholder.onRequest(player, params);

      // THEN
      assertEquals("0", result);
    }

    @Test
    void validPlayer() {
      // GIVEN
      String params = "portfolio-value";
      UUID uuid = UUID.randomUUID();
      BigDecimal portfolioValue = BigDecimal.valueOf(1100);

      OfflinePlayer player = mock(OfflinePlayer.class);
      StockPlayer stockPlayer = mock(StockPlayer.class);

      when(player.isOnline())
          .thenReturn(true);

      when(player.getUniqueId())
          .thenReturn(uuid);

      when(stockPlayer.getPortfolioValue())
          .thenReturn(portfolioValue);

      when(playerManager.getStockPlayer(uuid))
          .thenReturn(stockPlayer);

      when(settings.formatSigFig(portfolioValue))
          .thenReturn("1.1k");

      // WHEN
      String result = stockPlaceholder.onRequest(player, params);

      // THEN
      assertEquals("1.1k", result);
    }
  }

  @Nested
  class StockData {

    @Test
    void belowRequiredArgs() {
      // GIVEN
      String params = "sd-invalid";

      // WHEN
      String result = stockPlaceholder.onRequest(null, params);

      // THEN
      assertNull(result);
    }

    @Test
    void invalidStock() {
      // GIVEN
      String params = "sd-ba-name";

      when(stockPlaceholderManager.getPlaceholderStock("ba"))
          .thenReturn(null);

      // WHEN
      String result = stockPlaceholder.onRequest(null, params);

      // THEN
      assertEquals("N/A", result);
    }

    @MethodSource
    @ParameterizedTest
    void stockParams(String params, String expected) {
      // GIVEN
      when(stockPlaceholderManager.getPlaceholderStock("ba"))
          .thenReturn(buildPlaceholderStock());

      when(settings.formatSigFig(BigDecimal.valueOf(1_987_000_000)))
          .thenReturn("2.0b");

      when(settings.formatSigFig(1_100_000L))
          .thenReturn("1.1m");

      // WHEN
      String result = stockPlaceholder.onRequest(null, params);

      // THEN
      assertEquals(expected, result);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> stockParams() {
      return Stream.of(
          Arguments.of("sd-ba-NAME", "Boeing"),
          Arguments.of("sd-ba-cap", "2.0b"),
          Arguments.of("sd-ba-sp", "1000"),
          Arguments.of("sd-ba-vol", "1.1m"),
          Arguments.of("sd-ba-invalid", null)
      );
    }

    private PlaceholderStock buildPlaceholderStock() {
      Stock stock = new Stock("BA");
      stock.setName("Boeing");

      StockQuote quote = new StockQuote("BA");
      quote.setVolume(1_100_000L);
      stock.setQuote(quote);

      StockStats stockStats = new StockStats("BA");
      stockStats.setMarketCap(BigDecimal.valueOf(1_987_000_000));
      stock.setStats(stockStats);

      PlaceholderStock placeholderStock = new PlaceholderStock();
      placeholderStock.setStock(stock);
      placeholderStock.setServerPrice("1000");

      return placeholderStock;
    }
  }
}
