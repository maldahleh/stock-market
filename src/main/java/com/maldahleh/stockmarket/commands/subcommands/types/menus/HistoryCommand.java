package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public record HistoryCommand(Plugin plugin, InventoryManager inventoryManager,
                             Messages messages) implements
    Subcommand {

  @Override
  public void onCommand(Player player, String[] args) {
    if (args.length == 1) {
      messages.sendPendingHistory(player);
      inventoryManager.openStockHistoryInventory(player);
    } else {
      messages.sendPendingHistorySymbol(player);
      inventoryManager.openStockHistoryInventory(player, args[1]);
    }
  }

  @Override
  public int minArgs() {
    return 1;
  }

  @Override
  public int maxArgs() {
    return 2;
  }

  @Override
  public String requiredPerm() {
    return "stockmarket.history";
  }
}

