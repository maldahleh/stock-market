package com.maldahleh.stockmarket.commands.impl;

import com.maldahleh.stockmarket.commands.CommandManager;
import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public record StockMarketTabCompleter(CommandManager commandManager) implements TabCompleter {

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

    if (!CommandManager.hasBaseCommandPermission(player)) {
      return new ArrayList<>();
    }

    if (strings.length == 2) {
      if (shouldReturnPlayerList(strings[0], player)) {
        return null;
      }

      return new ArrayList<>();
    }

    List<String> possibleMatches = buildPossibleMatchesList(player);

    List<String> completions = new ArrayList<>();
    StringUtil.copyPartialMatches(strings[0], possibleMatches, completions);
    Collections.sort(completions);

    return completions;
  }

  private boolean shouldReturnPlayerList(String firstArg, Player player) {
    if (firstArg.equalsIgnoreCase("portfolio")
        && player.hasPermission("stockmarket.portfolio.other")) {
      return true;
    }

    return firstArg.equalsIgnoreCase("transactions")
        && player.hasPermission("stockmarket.transactions.other");
  }

  private List<String> buildPossibleMatchesList(Player player) {
    return commandManager.getRegisteredSubcommands().stream()
        .filter(subcommand -> hasPermission(player, subcommand))
        .map(Subcommand::commandName)
        .toList();
  }

  private boolean hasPermission(Player player, Subcommand subcommand) {
    return player.hasPermission(subcommand.requiredPerm());
  }
}
