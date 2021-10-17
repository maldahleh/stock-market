package com.maldahleh.stockmarket.inventories.history;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.history.providers.StockHistoryProvider;
import com.maldahleh.stockmarket.inventories.utils.paged.PagedInventory;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class StockHistoryInventory {

  private final PagedInventory<String, Transaction, UUID, Transaction, OfflinePlayer> inventory;

  public StockHistoryInventory(StockMarket stockMarket, Storage storage,
      Settings settings, ConfigurationSection section) {
    inventory = new PagedInventory<>(stockMarket, new StockHistoryProvider(stockMarket,
        storage, settings), section);
  }

  public void openInventory(Player player, String target) {
    inventory.displayInventory(player, target);
  }
}
