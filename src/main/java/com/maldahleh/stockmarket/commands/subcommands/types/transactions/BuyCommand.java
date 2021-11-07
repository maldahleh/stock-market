package com.maldahleh.stockmarket.commands.subcommands.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.common.TransactionCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;
import org.bukkit.entity.Player;

public class BuyCommand extends TransactionCommand {

  public BuyCommand(StockProcessor stockProcessor, Messages messages) {
    super(stockProcessor, messages);
  }

  @Override
  public void sendTransactionMessage(Player player) {
    messages.sendPendingBuy(player);
  }

  @Override
  public String commandName() {
    return "buy";
  }
}
