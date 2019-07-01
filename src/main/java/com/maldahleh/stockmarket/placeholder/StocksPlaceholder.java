package com.maldahleh.stockmarket.placeholder;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.stocks.wrapper.PlaceholderStock;
import com.maldahleh.stockmarket.utils.Utils;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

public class StocksPlaceholder extends PlaceholderExpansion {
  private PlayerManager playerManager;
  private StockManager stockManager;

  @Override
  public String getIdentifier() {
    return "sm";
  }

  @Override
  public String getAuthor() {
    return "maldahleh";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public String getRequiredPlugin() {
    return "StockMarket";
  }

  @Override
  public boolean canRegister() {
    return Bukkit.getPluginManager().getPlugin(getRequiredPlugin()) != null;
  }

  @Override
  public boolean register() {
    if (!canRegister()) {
      return false;
    }

    Plugin plugin = Bukkit.getPluginManager().getPlugin(getRequiredPlugin());
    if (plugin == null) {
      return false;
    }

    playerManager = ((StockMarket) plugin).getPlayerManager();
    stockManager = ((StockMarket) plugin).getStockManager();
    return PlaceholderAPI.registerPlaceholderHook(getIdentifier(), this);
  }

  @Override
  public String onRequest(OfflinePlayer p, String params) {
    if (params.startsWith("sd")) {
      String[] splitInfo = params.split("-");
      if (splitInfo.length != 3) {
        return null;
      }

      PlaceholderStock placeholderStock = stockManager.getPlaceholderStock(splitInfo[1]);
      if (placeholderStock == null) {
        return "N/A";
      }

      if (splitInfo[2].equalsIgnoreCase("name")) {
        return placeholderStock.getStock().getName();
      }

      if (splitInfo[2].equalsIgnoreCase("cap")) {
        return Utils.sigFigNumber(placeholderStock.getStock().getStats().getMarketCap()
            .doubleValue());
      }

      if (splitInfo[2].equalsIgnoreCase("sp")) {
        return placeholderStock.getServerPrice();
      }

      if (splitInfo[2].equalsIgnoreCase("vol")) {
        return Utils.sigFigNumber(placeholderStock.getStock().getQuote().getVolume());
      }

      return null;
    }

    if (params.equalsIgnoreCase("portfolio-value")) {
      if (p == null || !p.isOnline()) {
        return "Player Offline";
      }

      StockPlayer player = playerManager.getStockPlayer(p.getUniqueId());
      if (player == null) {
        return "0";
      }

      return Utils.sigFigNumber(player.getPortfolioValue().doubleValue());
    }

    return null;
  }
}
