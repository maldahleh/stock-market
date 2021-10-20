package com.maldahleh.stockmarket.inventories.transaction;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.transaction.provider.TransactionInventoryProvider;
import com.maldahleh.stockmarket.inventories.utils.paged.PagedInventory;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.time.Instant;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class TransactionInventory {

  private final PagedInventory<UUID, Instant, Transaction, Instant, Transaction> inventory;

  public TransactionInventory(
      StockMarket stockMarket,
      PlayerManager playerManager,
      Settings settings,
      ConfigurationSection section) {
    inventory =
        new PagedInventory<>(
            stockMarket,
            new TransactionInventoryProvider(stockMarket, playerManager, settings),
            section);
  }

  public void openInventory(Player player, UUID target) {
    inventory.displayInventory(player, target);
  }
}
