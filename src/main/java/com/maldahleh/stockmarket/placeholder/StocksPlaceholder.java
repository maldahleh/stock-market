package com.maldahleh.stockmarket.placeholder;

import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.stocks.wrapper.PlaceholderStock;
import com.maldahleh.stockmarket.utils.Utils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

@RequiredArgsConstructor
public class StocksPlaceholder extends PlaceholderExpansion {

  /**
   * Prefix for stock data placeholders
   * Example: sd-ba-vol (display the volume for BA - Boeing)
   */
  private static final String STOCK_DATA_PREFIX = "sd";
  /**
   * The minimum number of args required for a stock data placeholder
   * Arg 1 (index 0) - sd
   * Arg 2 (index 1) - symbol, ex: ba
   * Arg 3 (index 2) - data point, ex: vol
   */
  private static final int STOCK_DATA_REQ_ARGS = 3;
  private static final String STOCK_DATA_SEPARATOR = "-";

  private final PlayerManager playerManager;
  private final StockManager stockManager;

  @NonNull
  @Override
  public String getIdentifier() {
    return "sm";
  }

  @NonNull
  @Override
  public String getAuthor() {
    return "maldahleh";
  }

  @NonNull
  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public String onRequest(OfflinePlayer p, @NonNull String params) {
    String[] stockParams = getStockDataParams(params);
    if (stockParams.length != 0) {
      return getStockData(stockParams);
    }

    if (isPortfolioValue(params)) {
      return getPortfolioValue(p);
    }

    return null;
  }

  private boolean isPortfolioValue(String params) {
    return params.equalsIgnoreCase("portfolio-value");
  }

  private String getPortfolioValue(OfflinePlayer p) {
    if (p == null || !p.isOnline()) {
      return "Player Offline";
    }

    StockPlayer player = playerManager.getStockPlayer(p.getUniqueId());
    if (player == null) {
      return "0";
    }

    return Utils.sigFigNumber(player.getPortfolioValue().doubleValue());
  }

  private String[] getStockDataParams(String params) {
    if (!params.startsWith(STOCK_DATA_PREFIX)) {
      return new String[0];
    }

    return params.split(STOCK_DATA_SEPARATOR);
  }

  private String getStockData(String[] splitInfo) {
    if (splitInfo.length != STOCK_DATA_REQ_ARGS) {
      return null;
    }

    PlaceholderStock placeholderStock = stockManager.getPlaceholderStock(splitInfo[1]);
    if (placeholderStock == null) {
      return "N/A";
    }

    String dataPoint = splitInfo[2].toLowerCase();
    return switch (dataPoint) {
      case "name" -> placeholderStock.getStock().getName();
      case "cap" -> Utils.sigFigNumber(
          placeholderStock.getStock().getStats().getMarketCap().doubleValue());
      case "sp" -> placeholderStock.getServerPrice();
      case "vol" -> Utils.sigFigNumber(placeholderStock.getStock().getQuote().getVolume());
      default -> null;
    };
  }
}
