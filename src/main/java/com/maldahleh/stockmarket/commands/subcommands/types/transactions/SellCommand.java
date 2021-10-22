package com.maldahleh.stockmarket.commands.subcommands.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.common.TransactionCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;
import java.util.List;
import org.bukkit.entity.Player;

public class SellCommand extends TransactionCommand {

  public SellCommand(StockProcessor stockProcessor, Messages messages) {
    super(stockProcessor, messages);
  }

  @Override
  public void processTransaction(Player player, String symbol, int quantity) {
    messages.sendPendingSale(player);
    stockProcessor.sellStock(player, symbol, quantity);
  }

  @Override
  public String commandName() {
    return "sell";
  }

  @Override
  public List<String> commandHelpKeys(Player player) {
    return List.of("sell");
  }
}
