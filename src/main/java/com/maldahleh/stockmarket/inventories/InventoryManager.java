package com.maldahleh.stockmarket.inventories;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.compare.CompareInventory;
import com.maldahleh.stockmarket.inventories.list.ListInventory;
import com.maldahleh.stockmarket.inventories.lookup.LookupInventory;
import com.maldahleh.stockmarket.inventories.portfolio.PortfolioInventory;
import com.maldahleh.stockmarket.inventories.tutorial.TutorialInventory;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.stocks.StockManager;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class InventoryManager {
  private final LookupInventory lookupInventory;
  private final CompareInventory compareInventory;
  private final ListInventory listInventory;
  private final TutorialInventory tutorialInventory;
  private final PortfolioInventory portfolioInventory;

  public InventoryManager(StockMarket stockMarket, PlayerManager playerManager,
      StockManager stockManager, FileConfiguration config, Messages messages, Settings settings) {
    this.lookupInventory = new LookupInventory(stockMarket, stockManager, messages, settings,
        config.getConfigurationSection("inventories.lookup"));
    this.compareInventory = new CompareInventory(stockMarket, stockManager, messages, settings,
        config.getConfigurationSection("inventories.compare"));
    this.tutorialInventory = new TutorialInventory(stockMarket, config
        .getConfigurationSection("inventories.tutorial"));
    this.listInventory = new ListInventory(stockMarket, lookupInventory, config
        .getConfigurationSection("inventories.list"));
    this.portfolioInventory = new PortfolioInventory(stockMarket, playerManager, stockManager,
        settings, config.getConfigurationSection("inventories.portfolio"));
  }

  public void openLookupInventory(Player player, String symbol) {
    lookupInventory.openInventory(player, symbol);
  }

  public void openCompareInventory(Player player, String... symbols) {
    compareInventory.openInventory(player, symbols);
  }

  public void openListInventory(Player player) {
    listInventory.openInventory(player);
  }

  public void openTutorialInventory(Player player) {
    tutorialInventory.openInventory(player);
  }

  public void openPortfolioInventory(Player player) {
    portfolioInventory.openInventory(player, player.getUniqueId());
  }

  public void openPortfolioInventory(Player player, UUID target) {
    portfolioInventory.openInventory(player, target);
  }
}
