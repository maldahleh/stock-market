package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import java.util.List;
import org.bukkit.entity.Player;

public record LookupCommand(InventoryManager inventoryManager, Messages messages)
    implements Subcommand {

  @Override
  public void onCommand(Player player, String[] args) {
    messages.sendPendingLookup(player);
    inventoryManager.openLookupInventory(player, args[1]);
  }

  @Override
  public int minArgs() {
    return 2;
  }

  @Override
  public int maxArgs() {
    return 2;
  }

  @Override
  public String commandName() {
    return "lookup";
  }

  @Override
  public String requiredPerm() {
    return "stockmarket.lookup";
  }

  @Override
  public List<String> commandHelpKeys(Player player) {
    return List.of("lookup");
  }
}
