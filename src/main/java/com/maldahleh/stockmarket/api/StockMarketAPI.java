package com.maldahleh.stockmarket.api;

import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.players.player.data.StockData;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("unused")
public record StockMarketAPI(PlayerManager playerManager) {

  /**
   * Returns a player's portfolio value.
   *
   * @param uuid UUID of player to be looked up
   * @return Target player's portfolio value, -1 if not found
   */
  public BigDecimal getPortfolioValue(UUID uuid) {
    StockPlayer stockPlayer = playerManager.forceGetStockPlayer(uuid);
    if (stockPlayer == null) {
      return BigDecimal.valueOf(-1);
    }

    return stockPlayer.getPortfolioValue();
  }

  /**
   * Returns a player's profit margin.
   *
   * @param uuid UUID of player to be looked up
   * @return Target player's profit margin, -1 if not found
   */
  public BigDecimal getProfitMargin(UUID uuid) {
    StockPlayer stockPlayer = playerManager.forceGetStockPlayer(uuid);
    if (stockPlayer == null) {
      return BigDecimal.valueOf(-1);
    }

    return playerManager.getProfitMargin(stockPlayer, playerManager.getCurrentValue(stockPlayer));
  }

  /**
   * Return a map of all the stocks a player owns.
   *
   * @param uuid UUID of player to be looked up
   * @return Map of symbol -> stock data for player owned stocks
   */
  public Map<String, StockData> getPlayerStocks(UUID uuid) {
    StockPlayer stockPlayer = playerManager.forceGetStockPlayer(uuid);
    if (stockPlayer == null) {
      return new HashMap<>();
    }

    return stockPlayer.getStockMap();
  }
}
