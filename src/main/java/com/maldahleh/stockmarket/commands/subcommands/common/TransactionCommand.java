package com.maldahleh.stockmarket.commands.subcommands.common;

import com.maldahleh.stockmarket.commands.util.CommandUtils;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public abstract class TransactionCommand extends NoPermissionCommand {

  protected final StockProcessor stockProcessor;
  protected final Messages messages;

  public abstract void sendTransactionMessage(Player player);

  @Override
  public void onCommand(Player player, String[] args) {
    int quantity = CommandUtils.determineQuantity(args);
    if (quantity == CommandUtils.INVALID_QUANTITY) {
      messages.sendInvalidQuantity(player);
      return;
    }

    String symbol = args[1];
    sendTransactionMessage(player);
    stockProcessor.processTransaction(player, symbol, quantity);
  }

  @Override
  public int minArgs() {
    return 2;
  }

  @Override
  public int maxArgs() {
    return 3;
  }
}
