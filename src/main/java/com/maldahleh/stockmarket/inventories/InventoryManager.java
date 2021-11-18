package com.maldahleh.stockmarket.inventories;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.inventories.compare.CompareInventory;
import com.maldahleh.stockmarket.inventories.history.StockHistoryInventory;
import com.maldahleh.stockmarket.inventories.list.ListInventory;
import com.maldahleh.stockmarket.inventories.lookup.LookupInventory;
import com.maldahleh.stockmarket.inventories.portfolio.PortfolioInventory;
import com.maldahleh.stockmarket.inventories.transaction.TransactionInventory;
import com.maldahleh.stockmarket.inventories.tutorial.TutorialInventory;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import com.maldahleh.stockmarket.processor.types.SaleProcessor;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.storage.Storage;
import java.util.UUID;
import org.bukkit.entity.Player;

public class InventoryManager {

  private final LookupInventory lookupInventory;
  private final CompareInventory compareInventory;
  private final ListInventory listInventory;
  private final TutorialInventory tutorialInventory;
  private final PortfolioInventory portfolioInventory;
  private final TransactionInventory transactionInventory;
  private final StockHistoryInventory stockHistoryInventory;

  public InventoryManager(
      StockMarket stockMarket,
      PlayerManager playerManager,
      StockManager stockManager,
      PurchaseProcessor purchaseProcessor,
      SaleProcessor saleProcessor,
      Messages messages,
      Storage storage,
      Settings settings) {
    this.lookupInventory =
        new LookupInventory(
            stockMarket,
            stockManager,
            messages,
            settings,
            new ConfigSection(stockMarket, "lookup"));
    this.compareInventory =
        new CompareInventory(
            stockMarket,
            stockManager,
            messages,
            settings,
            new ConfigSection(stockMarket, "compare"));
    this.tutorialInventory =
        new TutorialInventory(stockMarket, new ConfigSection(stockMarket, "tutorial"));
    this.listInventory =
        new ListInventory(
            stockMarket,
            purchaseProcessor,
            saleProcessor,
            lookupInventory,
            new ConfigSection(stockMarket, "list"));
    this.portfolioInventory =
        new PortfolioInventory(
            stockMarket,
            messages,
            playerManager,
            stockManager,
            settings,
            new ConfigSection(stockMarket, "portfolio"));
    this.transactionInventory =
        new TransactionInventory(
            stockMarket,
            messages,
            playerManager,
            settings,
            new ConfigSection(stockMarket, "transactions"));
    this.stockHistoryInventory =
        new StockHistoryInventory(stockMarket, storage, messages, settings,
            new ConfigSection(stockMarket, "history"));
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
    portfolioInventory.displayInventory(player, player.getUniqueId());
  }

  public void openPortfolioInventory(Player player, UUID target) {
    portfolioInventory.displayInventory(player, target);
  }

  public void openTransactionInventory(Player player) {
    transactionInventory.displayInventory(player, player.getUniqueId());
  }

  public void openTransactionInventory(Player player, UUID target) {
    transactionInventory.displayInventory(player, target);
  }

  public void openStockHistoryInventory(Player player) {
    stockHistoryInventory.displayInventory(player, null);
  }

  public void openStockHistoryInventory(Player player, String symbol) {
    stockHistoryInventory.displayInventory(player, symbol);
  }
}
