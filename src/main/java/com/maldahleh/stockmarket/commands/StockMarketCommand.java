package com.maldahleh.stockmarket.commands;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import com.maldahleh.stockmarket.processor.StockProcessor;
import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public record StockMarketCommand(Plugin plugin,
                                 BrokerManager brokerManager,
                                 StockProcessor stockProcessor,
                                 InventoryManager inventoryManager,
                                 Messages messages) implements
    CommandExecutor {

  @Override
  @SuppressWarnings("java:S3516")
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

    messages.sendInvalidSyntax(player);
    return true;
  }
}
