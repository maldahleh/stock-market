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

public record StockMarketCommand(CommandManager commandManager,
                                 BrokerManager brokerManager,
                                 Messages messages) implements
    CommandExecutor {

  private static final String PLAYER_ONLY_MESSAGE = "Stocks - You must be a player to use this command.";

  @Override
  @SuppressWarnings("java:S3516")
  public boolean onCommand(@NonNull CommandSender commandSender, @NonNull Command command,
      @NonNull String s, @NonNull String[] strings) {
    if (!(commandSender instanceof Player player)) {
      commandSender.sendMessage(PLAYER_ONLY_MESSAGE);
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

    Subcommand subcommand = commandManager.findSubcommand(strings[1]);
    if (!canPlayerExecuteSubcommand(player, subcommand)) {
      messages.sendNoPermission(player);
      return true;
    }

    if (!isValidSyntax(subcommand, strings)) {
      messages.sendInvalidSyntax(player);
      return true;
    }

    subcommand.onCommand(player, strings);
    return true;
  }

  private boolean canPlayerExecuteSubcommand(Player player, Subcommand subcommand) {
    if (!player.hasPermission(CommandManager.DEFAULT_PERM)) {
      return false;
    }

    String requiredPerm = subcommand.requiredPerm();
    if (requiredPerm == null) {
      return true;
    }

    return player.hasPermission(requiredPerm);
  }

  private boolean isValidSyntax(Subcommand subcommand, String[] args) {
    return args.length >= subcommand.minArgs() && args.length <= subcommand.maxArgs();
  }
}
