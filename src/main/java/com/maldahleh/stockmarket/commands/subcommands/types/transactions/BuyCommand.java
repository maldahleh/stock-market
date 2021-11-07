package com.maldahleh.stockmarket.commands.subcommands.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.common.TransactionCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import com.maldahleh.stockmarket.processor.types.SaleProcessor;
import org.bukkit.entity.Player;

public class BuyCommand extends TransactionCommand {

  public BuyCommand(PurchaseProcessor purchaseProcessor, SaleProcessor saleProcessor,
      Messages messages) {
    super(purchaseProcessor, saleProcessor, messages);
  }

  @Override
  public void processTransaction(Player player, String symbol, int quantity) {
    messages.sendPendingBuy(player);
    purchaseProcessor.processTransaction(player, symbol, quantity);
  }

  @Override
  public String commandName() {
    return "buy";
  }
}
