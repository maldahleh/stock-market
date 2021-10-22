package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import java.util.List;
import org.bukkit.entity.Player;

public record ListCommand(InventoryManager inventoryManager) implements Subcommand {

  @Override
  public void onCommand(Player player, String[] args) {
    inventoryManager.openListInventory(player);
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
    return "list";
  }

  @Override
  public String requiredPerm() {
    return "stockmarket.list";
  }

  @Override
  public List<String> commandHelpKeys(Player player) {
    return List.of("list");
  }
}
