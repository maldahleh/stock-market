package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class LookupCommand extends BaseCommand {

  private final InventoryManager inventoryManager;
  private final Messages messages;

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
}
