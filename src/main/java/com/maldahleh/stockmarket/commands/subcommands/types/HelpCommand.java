package com.maldahleh.stockmarket.commands.subcommands.types;

import com.maldahleh.stockmarket.commands.subcommands.common.NoPermissionCommand;
import com.maldahleh.stockmarket.config.Messages;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class HelpCommand extends NoPermissionCommand {

  private final Messages messages;

  @Override
  public void onCommand(Player player, String[] args) {
    messages.sendHelpMessage(player);
  }

  @Override
  public String commandName() {
    return "help";
  }
}
