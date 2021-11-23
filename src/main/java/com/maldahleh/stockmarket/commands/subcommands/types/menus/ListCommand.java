package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.entity.Player;

public class ListCommand extends BaseCommand {

  private final InventoryManager inventoryManager;

  public ListCommand(InventoryManager inventoryManager, Messages messages) {
    super(messages);

    this.inventoryManager = inventoryManager;
  }

  @Override
  public void onCommand(Player player, String[] args) {
    inventoryManager.openListInventory(player);
  }

  @Override
  public String commandName() {
    return "list";
  }
}
