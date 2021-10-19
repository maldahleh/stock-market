package com.maldahleh.stockmarket.commands.subcommands.types;

import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.config.Messages;
import org.bukkit.entity.Player;

public record HelpCommand(Messages messages) implements
    Subcommand {

  @Override
  public void onCommand(Player player, String[] args) {
    messages.sendHelpMessage(player);
  }

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
    return null;
  }
}
