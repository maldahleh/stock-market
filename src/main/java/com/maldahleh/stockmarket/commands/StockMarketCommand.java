package com.maldahleh.stockmarket.commands;

import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class StockMarketCommand implements CommandExecutor {
  private final InventoryManager inventoryManager;
  private final Messages messages;

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s,
      String[] strings) {
    if (!(commandSender instanceof Player)) {
      commandSender.sendMessage("Stocks - You must be a player to use this command.");
      return true;
    }

    Player player = (Player) commandSender;
    if (!player.hasPermission("stockmarket.use")) {
      messages.sendNoPermission(player);
      return true;
    }

    if (strings.length == 1 && strings[0].equalsIgnoreCase("list")) {
      if (!player.hasPermission("stockmarket.list")) {
        messages.sendNoPermission(player);
        return true;
      }

      inventoryManager.openListInventory(player);
      return true;
    }

    if (strings.length == 1 && strings[0].equalsIgnoreCase("tutorial")) {
      if (!player.hasPermission("stockmarket.tutorial")) {
        messages.sendNoPermission(player);
        return true;
      }

      inventoryManager.openTutorialInventory(player);
      return true;
    }

    if (strings.length == 2 && strings[0].equalsIgnoreCase("lookup")) {
      if (!player.hasPermission("stockmarket.lookup")) {
        messages.sendNoPermission(player);
        return true;
      }

      inventoryManager.openLookupInventory(player, strings[1]);
      return true;
    }

    return false;
  }
}
