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

@SuppressWarnings("java:S1168")
public record StockMarketTabCompleter(CommandManager commandManager) implements TabCompleter {

  private static final int COMMAND_INDEX = 0;

  @Override
  public List<String> onTabComplete(@NonNull CommandSender commandSender, @NonNull Command command,
      @NonNull String s, @NonNull String[] strings) {
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
      return shouldReturnPlayerList(player, commandName);
    }

    return findPossibleMatches(player, commandName);
  }

  private List<String> shouldReturnPlayerList(Player player, String commandName) {
    boolean shouldReturnPlayerList = commandManager.getRegisteredSubcommands().stream()
        .filter(subcommand -> subcommand.commandName().equalsIgnoreCase(commandName))
        .anyMatch(subcommand -> subcommand.shouldTabCompleterReturnPlayerList(player));
    if (shouldReturnPlayerList) {
      return null;
    }

    return new ArrayList<>();
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