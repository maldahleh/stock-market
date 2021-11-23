package com.maldahleh.stockmarket.commands.subcommands.types;

import com.maldahleh.stockmarket.commands.subcommands.common.NoPermissionCommand;
import com.maldahleh.stockmarket.config.Messages;
import org.bukkit.entity.Player;

public class HelpCommand extends NoPermissionCommand {

  public HelpCommand(Messages messages) {
    super(messages);
  }

  @Override
  public void onCommand(Player player, String[] args) {
    messages.sendHelpMessage(player);
  }

  @Override
  public String commandName() {
    return "help";
  }
}
