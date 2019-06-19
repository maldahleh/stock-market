package com.maldahleh.stockmarket.commands;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class StockMarketTabCompleter implements TabCompleter {

  @Override
  public List<String> onTabComplete(CommandSender commandSender, Command command, String s,
      String[] strings) {
    if (!(commandSender instanceof Player)) {
      return null;
    }


    return null;
  }
}
