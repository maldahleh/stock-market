package com.maldahleh.stockmarket.commands.subcommands.types.broker;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class SpawnSimpleBrokerCommand extends BaseCommand {

  private static final String NOT_ENABLED_MESSAGE =
      ChatColor.RED + "Citizens is not enabled, and is required for brokers";

  private final BrokerManager brokerManager;

  @Override
  public void onCommand(Player player, String[] args) {
    if (!brokerManager.isEnabled()) {
      player.sendMessage(NOT_ENABLED_MESSAGE);
      return;
    }

    brokerManager.spawnSimpleBroker(player.getLocation());
  }

  @Override
  public String commandName() {
    return "simplebroker";
  }
}
