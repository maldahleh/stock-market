package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.common.TargetableCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TransactionsCommand extends TargetableCommand {

  public TransactionsCommand(Plugin plugin, InventoryManager inventoryManager, Messages messages) {
    super(plugin, inventoryManager, messages);
  }

  @Override
  public void callerAction(Player caller) {
    messages.sendPendingTransactions(caller);
    inventoryManager.openTransactionInventory(caller);
  }

  @Override
  public void targetAction(Player caller, UUID target) {
    messages.sendPendingTransactionsOther(caller);
    inventoryManager.openTransactionInventory(caller, target);
  }

  @Override
  public String commandName() {
    return "transactions";
  }
}
