package com.maldahleh.stockmarket.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.players.player.data.StockData;
import java.math.BigDecimal;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class StockMarketAPITests {

  private UUID uuid;
  private StockPlayer stockPlayer;
  private PlayerManager playerManager;
  private StockMarketAPI stockMarketAPI;

  @BeforeEach
  void setup() {
    uuid = UUID.randomUUID();
    stockPlayer = mock(StockPlayer.class);
    playerManager = mock(PlayerManager.class);
    stockMarketAPI = new StockMarketAPI(playerManager);
  }

  @Nested
  class PortfolioValue {

    @Test
    void notFoundPlayer() {
      // GIVEN
      when(playerManager.forceGetStockPlayer(uuid))
          .thenReturn(null);

      // WHEN
      BigDecimal portfolioValue = stockMarketAPI.getPortfolioValue(uuid);

      // THEN
      assertEquals(BigDecimal.valueOf(-1), portfolioValue);
    }

    @Test
    void foundPlayer() {
      // GIVEN
      when(stockPlayer.getPortfolioValue())
          .thenReturn(BigDecimal.TEN);

      when(playerManager.forceGetStockPlayer(uuid))
          .thenReturn(stockPlayer);

      // WHEN
      BigDecimal portfolioValue = stockMarketAPI.getPortfolioValue(uuid);

      // THEN
      assertEquals(BigDecimal.TEN, portfolioValue);
    }
  }

  @Nested
  class ProfitMargin {

    @Test
    void notFoundPlayer() {
      // GIVEN
      when(playerManager.forceGetStockPlayer(uuid))
          .thenReturn(null);

      // WHEN
      BigDecimal profitMargin = stockMarketAPI.getProfitMargin(uuid);

      // THEN
      assertEquals(BigDecimal.valueOf(-1), profitMargin);
    }

    @Test
    void foundPlayer() {
      // GIVEN
      when(playerManager.getCurrentValue(stockPlayer))
          .thenReturn(BigDecimal.ONE);

      when(stockPlayer.getProfitMargin(BigDecimal.ONE))
          .thenReturn(BigDecimal.valueOf(11));

      when(playerManager.forceGetStockPlayer(uuid))
          .thenReturn(stockPlayer);

      // WHEN
      BigDecimal profitMargin = stockMarketAPI.getProfitMargin(uuid);

      // THEN
      assertEquals(BigDecimal.valueOf(11), profitMargin);
    }
  }

  @Nested
  class PlayerStocks {

    @Test
    void notFoundPlayer() {
      // GIVEN
      when(playerManager.forceGetStockPlayer(uuid))
          .thenReturn(null);

      // WHEN
      Map<String, StockData> playerStocks = stockMarketAPI.getPlayerStocks(uuid);

      // THEN
      assertEquals(0, playerStocks.size());
    }

    @Test
    void foundPlayer() {
      // GIVEN
      NavigableMap<String, StockData> stockMap = new TreeMap<>();
      stockMap.put("BA", new StockData());

      when(stockPlayer.getStockMap())
          .thenReturn(stockMap);

      when(playerManager.forceGetStockPlayer(uuid))
          .thenReturn(stockPlayer);

      // WHEN
      Map<String, StockData> playerStocks = stockMarketAPI.getPlayerStocks(uuid);

      // THEN
      assertEquals(1, playerStocks.size());
      assertTrue(playerStocks.containsKey("BA"));
    }
  }
}
