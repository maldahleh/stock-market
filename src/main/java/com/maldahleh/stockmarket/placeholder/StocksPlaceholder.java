package com.maldahleh.stockmarket.placeholder;

import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.stocks.wrapper.PlaceholderStock;
import com.maldahleh.stockmarket.utils.CurrencyUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

@RequiredArgsConstructor
public class StocksPlaceholder extends PlaceholderExpansion {

  private static final String STOCK_IDENTIFIER = "sm";
  private static final String NAME_DATA_POINT = "name";
  private static final String MARKET_CAP_DATA_POINT = "cap";
  private static final String SERVER_PRICE_DATA_POINT = "sp";
  private static final String VOLUME_DATA_POINT = "vol";
  private static final String PORTFOLIO_VALUE_POINT = "portfolio-value";

  /**
   * Prefix for stock data placeholders Example: sd-ba-vol (display the volume for BA - Boeing)
   */
  private static final String STOCK_DATA_PREFIX = "sd";
  /**
   * The minimum number of args required for a stock data placeholder Arg 1 (index 0) - sd Arg 2
   * (index 1) - symbol, ex: ba Arg 3 (index 2) - data point, ex: vol
   */
  private static final int STOCK_DATA_REQ_ARGS = 3;

  private static final int DATA_POINT_NAME_INDEX = 2;
  private static final String STOCK_DATA_SEPARATOR = "-";

  private static final String ZERO_VALUE = "0";
  private static final String OFFLINE_PLAYER = "Player Offline";
  private static final String NOT_APPLICABLE = "N/A";

  private final PlayerManager playerManager;
  private final StockManager stockManager;

  @NonNull
  @Override
  public String getIdentifier() {
    return STOCK_IDENTIFIER;
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
    return params.equalsIgnoreCase(PORTFOLIO_VALUE_POINT);
  }

  private String getPortfolioValue(OfflinePlayer p) {
    if (p == null || !p.isOnline()) {
      return OFFLINE_PLAYER;
    }

    StockPlayer player = playerManager.getStockPlayer(p.getUniqueId());
    if (player == null) {
      return ZERO_VALUE;
    }

    return CurrencyUtils.sigFigNumber(player.getPortfolioValue());
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
      return NOT_APPLICABLE;
    }

    String dataPoint = splitInfo[DATA_POINT_NAME_INDEX].toLowerCase();
    return switch (dataPoint) {
      case NAME_DATA_POINT -> placeholderStock.getStock().getName();
      case MARKET_CAP_DATA_POINT -> CurrencyUtils.sigFigNumber(
          placeholderStock.getStock().getStats().getMarketCap());
      case SERVER_PRICE_DATA_POINT -> placeholderStock.getServerPrice();
      case VOLUME_DATA_POINT -> CurrencyUtils.sigFigNumber(
          placeholderStock.getStock().getQuote().getVolume());
      default -> null;
    };
  }
}
