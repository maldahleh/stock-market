package com.maldahleh.stockmarket.commands.impl;

import com.maldahleh.stockmarket.commands.CommandManager;
import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public record StockMarketTabCompleter(CommandManager commandManager) implements TabCompleter {

  private static final int COMMAND_INDEX = 0;

  @Override
  @SuppressWarnings("java:S1168")
  public List<String> onTabComplete(
      @NonNull CommandSender commandSender,
      @NonNull Command command,
      @NonNull String s,
      @NonNull String[] strings) {
    if (!(commandSender instanceof Player player)) {
      return new ArrayList<>();
    }

    if (strings.length < 1 || strings.length > 2) {
      return new ArrayList<>();
    }

    if (CommandManager.doesNotHaveBasePermission(player)) {
      return new ArrayList<>();
    }

    String commandName = strings[COMMAND_INDEX];
    if (strings.length == 2) {
      if (shouldReturnPlayerList(commandName, player)) {
        return null;
      }

      return new ArrayList<>();
    }

    return findPossibleMatches(player, commandName);
  }

  private boolean shouldReturnPlayerList(String firstArg, Player player) {
    if (firstArg.equalsIgnoreCase("portfolio")
        && player.hasPermission("stockmarket.portfolio.other")) {
      return true;
    }

    return firstArg.equalsIgnoreCase("transactions")
        && player.hasPermission("stockmarket.transactions.other");
  }

  private List<String> findPossibleMatches(Player player, String arg) {
    return commandManager.getRegisteredSubcommands().stream()
        .filter(subcommand -> subcommand.canPlayerExecute(player))
        .map(Subcommand::commandName)
        .filter(subcommand -> startsWith(subcommand, arg))
        .sorted()
        .toList();
  }

  private boolean startsWith(String command, String prefix) {
    return command.length() >= prefix.length() && command.regionMatches(true, 0, prefix, 0,
        prefix.length());
  }
}
