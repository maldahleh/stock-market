package com.maldahleh.stockmarket.commands;

import com.maldahleh.stockmarket.config.Messages;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class StockMarketCommand implements CommandExecutor {
  private final Messages messages;

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s,
      String[] strings) {
    if (!(commandSender instanceof Player)) {
      commandSender.sendMessage("Stocks - You must be a player to use this command.");
      return true;
    }

    Player player = (Player) commandSender;
    if (!commandSender.hasPermission("stockmarket.use")) {
      messages.sendNoPermission(player);
      return true;
    }


    return false;
  }
}
