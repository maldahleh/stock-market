package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.entity.Player;

public class CompareCommand extends BaseCommand {

  private static final String SEPARATOR = ",";
  private static final int MAX_SYMBOLS = 3;

  private final InventoryManager inventoryManager;

  public CompareCommand(InventoryManager inventoryManager, Messages messages) {
    super(messages);

    this.inventoryManager = inventoryManager;
  }

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

    sendPending(player);
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
