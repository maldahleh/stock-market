package com.maldahleh.stockmarket.commands.subcommands.types.broker;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.config.Messages;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class SpawnSimpleBrokerCommand extends BaseCommand {

  private final BrokerManager brokerManager;
  private final Messages messages;

  @Override
  public void onCommand(Player player, String[] args) {
    if (!brokerManager.isEnabled()) {
      messages.sendCitizensRequired(player);
      return;
    }

    brokerManager.spawnSimpleBroker(player.getLocation());
  }

  @Override
  public String commandName() {
    return "simplebroker";
  }
}
