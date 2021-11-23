package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import java.util.List;
import org.bukkit.entity.Player;

public class HistoryCommand extends BaseCommand {

  private final InventoryManager inventoryManager;

  public HistoryCommand(InventoryManager inventoryManager, Messages messages) {
    super(messages);

    this.inventoryManager = inventoryManager;
  }

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
  public int maxArgs() {
    return 2;
  }

  @Override
  public String commandName() {
    return "history";
  }

  @Override
  public List<String> commandHelpKeys(Player player) {
    return List.of("history", "history-symbol");
  }
}
