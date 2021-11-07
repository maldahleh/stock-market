package com.maldahleh.stockmarket.commands.subcommands.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.common.TransactionCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;
import org.bukkit.entity.Player;

public class SellCommand extends TransactionCommand {

  public SellCommand(StockProcessor stockProcessor, Messages messages) {
    super(stockProcessor, messages);
  }

  @Override
  public void sendTransactionMessage(Player player) {
    messages.sendPendingSale(player);
  }

  @Override
  public String commandName() {
    return "sell";
  }
}
