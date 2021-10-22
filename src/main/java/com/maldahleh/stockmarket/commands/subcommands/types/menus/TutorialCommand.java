package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class TutorialCommand extends BaseCommand {

  private final InventoryManager inventoryManager;

  @Override
  public void onCommand(Player player, String[] args) {
    inventoryManager.openTutorialInventory(player);
  }

  @Override
  public String commandName() {
    return "tutorial";
  }
}
