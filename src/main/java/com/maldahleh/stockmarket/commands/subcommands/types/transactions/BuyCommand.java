package com.maldahleh.stockmarket.commands.subcommands.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.commands.util.CommandUtils;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;
import org.bukkit.entity.Player;

public record BuyCommand(StockProcessor stockProcessor,
                         Messages messages) implements Subcommand {

  @Override
  public void onCommand(Player player, String[] args) {
    int quantity = CommandUtils.determineQuantity(args);
    if (quantity == -1) {
      messages.sendInvalidQuantity(player);
      return;
    }

    messages.sendPendingBuy(player);
    stockProcessor.buyStock(player, args[1], quantity);
  }

  @Override
  public int minArgs() {
    return 2;
  }

  @Override
  public int maxArgs() {
    return 3;
  }

  @Override
  public String requiredPerm() {
    return null;
  }
}
