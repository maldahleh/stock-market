package com.maldahleh.stockmarket.commands.subcommands.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.common.TransactionCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;
import java.util.List;
import org.bukkit.entity.Player;

public class BuyCommand extends TransactionCommand {

  public BuyCommand(StockProcessor stockProcessor, Messages messages) {
    super(stockProcessor, messages);
  }

  @Override
  public void processTransaction(Player player, String symbol, int quantity) {
    messages.sendPendingBuy(player);
    stockProcessor.buyStock(player, symbol, quantity);
  }

  @Override
  public String commandName() {
    return "buy";
  }

  @Override
  public List<String> commandHelpKeys(Player player) {
    return List.of("buy");
  }
}
