package com.maldahleh.stockmarket.commands.subcommands.common;

import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import java.util.List;
import org.bukkit.entity.Player;

public abstract class BaseCommand implements Subcommand {

  @Override
  public int minArgs() {
    return 1;
  }

  @Override
  public int maxArgs() {
    return 1;
  }

  @Override
  public String requiredPerm() {
    return "stockmarket." + commandName();
  }

  @Override
  public List<String> commandHelpKeys(Player player) {
    return List.of(commandName());
  }

  @Override
  public boolean canPlayerExecute(Player player) {
    return requiredPerm() == null || player.hasPermission(requiredPerm());
  }

  @Override
  public boolean shouldTabCompleterReturnPlayerList(Player player) {
    return false;
  }
}
