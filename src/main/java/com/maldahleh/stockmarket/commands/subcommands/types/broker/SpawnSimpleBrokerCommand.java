package com.maldahleh.stockmarket.commands.subcommands.types.broker;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.config.Messages;
import org.bukkit.entity.Player;

public class SpawnSimpleBrokerCommand extends BaseCommand {

  private final BrokerManager brokerManager;

  public SpawnSimpleBrokerCommand(BrokerManager brokerManager, Messages messages) {
    super(messages);

    this.brokerManager = brokerManager;
  }

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
