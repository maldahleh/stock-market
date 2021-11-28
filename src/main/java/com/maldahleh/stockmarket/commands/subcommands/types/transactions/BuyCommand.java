package com.maldahleh.stockmarket.commands.subcommands.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.common.TransactionCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;

public class BuyCommand extends TransactionCommand {

  public BuyCommand(StockProcessor stockProcessor, Messages messages) {
    super(stockProcessor, messages);
  }

  @Override
  public String commandName() {
    return "buy";
  }
}
