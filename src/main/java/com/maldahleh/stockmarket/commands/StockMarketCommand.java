package com.maldahleh.stockmarket.commands;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import com.maldahleh.stockmarket.processor.StockProcessor;
import com.maldahleh.stockmarket.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@AllArgsConstructor
public record StockMarketCommand(Plugin plugin,
                                 BrokerManager brokerManager,
                                 StockProcessor stockProcessor,
                                 InventoryManager inventoryManager,
                                 Messages messages) implements
    CommandExecutor {

  @Override
  public boolean onCommand(@NonNull CommandSender commandSender, @NonNull Command command,
      @NonNull String s,
      @NonNull String[] strings) {
    if (!(commandSender instanceof Player player)) {
      commandSender.sendMessage("Stocks - You must be a player to use this command.");
      return true;
    }

    if (!player.hasPermission("stockmarket.use")) {
      messages.sendNoPermission(player);
      return true;
    }

    if (brokerManager.areCommandsDisabled(player)) {
      messages.sendCommandsDisabled(player);
      return true;
    }

    if (strings.length == 0) {
      messages.sendHelpMessage(player);
      return true;
    }

    if (strings.length == 2 && strings[0].equalsIgnoreCase("compare")
        && strings[1].contains(",")) {
      if (!player.hasPermission("stockmarket.compare")) {
        messages.sendNoPermission(player);
        return true;
      }

      String[] symbols = strings[1].split(",");
      if (symbols.length > 3) {
        messages.sendCompareMax(player);
        return true;
      }

      messages.sendPendingCompare(player);
      inventoryManager.openCompareInventory(player, symbols);
      return true;
    }

    if (strings.length == 1 && strings[0].equalsIgnoreCase("history")) {
      if (!player.hasPermission("stockmarket.history")) {
        messages.sendNoPermission(player);
        return true;
      }

      messages.sendPendingHistory(player);
      inventoryManager.openStockHistoryInventory(player);
      return true;
    }

    if (strings.length == 2 && strings[0].equalsIgnoreCase("history")) {
      if (!player.hasPermission("stockmarket.history")) {
        messages.sendNoPermission(player);
        return true;
      }

      messages.sendPendingHistorySymbol(player);
      inventoryManager.openStockHistoryInventory(player, strings[1]);
      return true;
    }

    if ((strings.length == 2 || strings.length == 3)
        && strings[0].equalsIgnoreCase("buy")) {
      Integer quantity = 1;

      if (strings.length == 3) {
        quantity = Utils.getInteger(strings[2]);
        if (quantity == null || quantity <= 0) {
          messages.sendInvalidQuantity(player);
          return true;
        }
      }

      messages.sendPendingBuy(player);
      stockProcessor.buyStock(player, strings[1], quantity);
      return true;
    }

    if ((strings.length == 2 || strings.length == 3)
        && strings[0].equalsIgnoreCase("sell")) {
      Integer quantity = 1;

      if (strings.length == 3) {
        quantity = Utils.getInteger(strings[2]);
        if (quantity == null || quantity <= 0) {
          messages.sendInvalidQuantity(player);
          return true;
        }
      }

      messages.sendPendingSale(player);
      stockProcessor.sellStock(player, strings[1], quantity);
      return true;
    }

    messages.sendInvalidSyntax(player);
    return true;
  }
}
