package com.maldahleh.stockmarket.inventories.portfolio;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.inventories.portfolio.provider.PortfolioInventoryProvider;
import com.maldahleh.stockmarket.inventories.utils.paged.PagedInventory;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.data.StockData;
import com.maldahleh.stockmarket.stocks.StockManager;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import yahoofinance.Stock;

public class PortfolioInventory {

  private final PagedInventory<UUID, String, StockData, Stock, StockData> inventory;

  public PortfolioInventory(StockMarket stockMarket, PlayerManager playerManager,
      StockManager stockManager, Settings settings, ConfigSection section) {
    inventory = new PagedInventory<>(stockMarket, new PortfolioInventoryProvider(stockMarket,
        playerManager, stockManager, settings), section);
  }

  public void openInventory(Player player, UUID target) {
    inventory.displayInventory(player, target);
  }
}
