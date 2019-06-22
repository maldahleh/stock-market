package com.maldahleh.stockmarket.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class StockMarketTabCompleter implements TabCompleter {

  @Override
  public List<String> onTabComplete(CommandSender commandSender, Command command, String s,
      String[] strings) {
    if (!(commandSender instanceof Player)) {
      return new ArrayList<>();
    }

    if (strings.length != 1 && strings.length != 2) {
      return new ArrayList<>();
    }

    Player player = (Player) commandSender;
    if (!player.hasPermission("stockmarket.use")) {
      return new ArrayList<>();
    }

    if (strings.length == 2) {
      if (strings[0].equalsIgnoreCase("portfolio")
          && player.hasPermission("stockmarket.portfolio.other")) {
        return null;
      }

      if (strings[0].equalsIgnoreCase("transactions")
          && player.hasPermission("stockmarket.transactions.other")) {
        return null;
      }

      return new ArrayList<>();
    }

    List<String> possibleMatches = new ArrayList<>();
    possibleMatches.add("buy");
    possibleMatches.add("sell");

    if (player.hasPermission("stockmarket.spawnbroker")) {
      possibleMatches.add("spawnsimplebroker");
    }

    if (player.hasPermission("stockmarket.list")) {
      possibleMatches.add("list");
    }

    if (player.hasPermission("stockmarket.tutorial")) {
      possibleMatches.add("tutorial");
    }

    if (player.hasPermission("stockmarket.lookup")) {
      possibleMatches.add("lookup");
    }

    if (player.hasPermission("stockmarket.compare")) {
      possibleMatches.add("compare");
    }

    if (player.hasPermission("stockmarket.portfolio")
        || player.hasPermission("stockmarket.portfolio.other")) {
      possibleMatches.add("portfolio");
    }

    if (player.hasPermission("stockmarket.transactions")
        || player.hasPermission("stockmarket.transactions.other")) {
      possibleMatches.add("transactions");
    }

    if (player.hasPermission("stockmarket.history")) {
      possibleMatches.add("history");
    }

    List<String> completions = new ArrayList<>();
    StringUtil.copyPartialMatches(strings[0], possibleMatches, completions);
    Collections.sort(completions);

    return completions;
  }
}
