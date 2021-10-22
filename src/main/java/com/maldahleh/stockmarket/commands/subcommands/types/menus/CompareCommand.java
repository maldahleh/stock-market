package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class CompareCommand extends BaseCommand {

  private static final String SEPARATOR = ",";
  private static final int MAX_SYMBOLS = 3;

  private final InventoryManager inventoryManager;
  private final Messages messages;

  @Override
  public void onCommand(Player player, String[] args) {
    String[] symbols = args[1].split(SEPARATOR);
    if (symbols.length == 1) {
      messages.sendInvalidSyntax(player);
      return;
    }

    if (symbols.length > MAX_SYMBOLS) {
      messages.sendCompareMax(player);
      return;
    }

    messages.sendPendingCompare(player);
    inventoryManager.openCompareInventory(player, symbols);
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
    return "compare";
  }
}
