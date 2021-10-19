package com.maldahleh.stockmarket.commands.subcommands;

import org.bukkit.entity.Player;

public interface Subcommand {

  void onCommand(Player player, String[] args);

  int minArgs();

  int maxArgs();

  String commandName();

  String requiredPerm();
}
