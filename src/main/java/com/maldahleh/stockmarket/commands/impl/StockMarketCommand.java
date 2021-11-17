package com.maldahleh.stockmarket.commands.impl;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.CommandManager;
import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.config.Messages;
import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public record StockMarketCommand(CommandManager commandManager, BrokerManager brokerManager,
                                 Messages messages) implements CommandExecutor {

  @Override
  @SuppressWarnings("java:S3516")
  public boolean onCommand(
      @NonNull CommandSender commandSender,
      @NonNull Command command,
      @NonNull String s,
      @NonNull String[] strings) {
    if (!(commandSender instanceof Player player)) {
      messages.sendPlayerOnly(commandSender);
      return true;
    }

    if (CommandManager.doesNotHaveBasePermission(player)) {
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

    Subcommand subcommand = commandManager.findSubcommand(strings[0]);
    if (subcommand == null) {
      messages.sendInvalidSyntax(player);
      return true;
    }

    if (!subcommand.canPlayerExecute(player)) {
      messages.sendNoPermission(player);
      return true;
    }

    if (isInvalidSyntax(subcommand, strings)) {
      messages.sendInvalidSyntax(player);
      return true;
    }

    subcommand.onCommand(player, strings);
    return true;
  }

  private boolean isInvalidSyntax(Subcommand subcommand, String[] args) {
    return args.length < subcommand.minArgs() || args.length > subcommand.maxArgs();
  }
}
