package com.maldahleh.stockmarket.commands.subcommands.types.broker;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.config.Messages;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public record SpawnSimpleBrokerCommand(BrokerManager brokerManager, Messages messages)
    implements Subcommand {

  private static final String NOT_ENABLED_MESSAGE =
      ChatColor.RED + "Citizens is not enabled, and is required for brokers";

  @Override
  public void onCommand(Player player, String[] args) {
    if (!brokerManager.isEnabled()) {
      player.sendMessage(NOT_ENABLED_MESSAGE);
      return;
    }

    brokerManager.spawnSimpleBroker(player.getLocation());
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
  public String commandName() {
    return "spawnsimplebroker";
  }

  @Override
  public String requiredPerm() {
    return "stockmarket.spawnbroker";
  }

  @Override
  public List<String> commandHelpKeys(Player player) {
    return List.of("simplebroker");
  }
}
