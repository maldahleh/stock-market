package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.entity.Player;

public record TutorialCommand(
    InventoryManager inventoryManager) implements Subcommand {

  @Override
  public void onCommand(Player player, String[] args) {
    inventoryManager.openTutorialInventory(player);
  }

  @Override
  public int minArgs() {
    return 1;
  }

  @Override
  public int maxArgs() {
    return 1;
  }

  @Override
  public String commandName() {
    return "tutorial";
  }

  @Override
  public String requiredPerm() {
    return "stockmarket.tutorial";
  }
}

