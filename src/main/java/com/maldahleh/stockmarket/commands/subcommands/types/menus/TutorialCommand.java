package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.entity.Player;

public class TutorialCommand extends BaseCommand {

  private final InventoryManager inventoryManager;

  public TutorialCommand(InventoryManager inventoryManager, Messages messages) {
    super(messages);

    this.inventoryManager = inventoryManager;
  }

  @Override
  public void onCommand(Player player, String[] args) {
    inventoryManager.openTutorialInventory(player);
  }

  @Override
  public String commandName() {
    return "tutorial";
  }
}
