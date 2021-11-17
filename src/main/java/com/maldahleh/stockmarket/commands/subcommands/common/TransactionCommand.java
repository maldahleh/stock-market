package com.maldahleh.stockmarket.commands.subcommands.common;

import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public abstract class TransactionCommand extends NoPermissionCommand {

  /**
   * The minimum number of args we need to look up a quantity. This is because the user supplied
   * quantity is always the 2nd index (3rd argument).
   */
  private static final int MIN_ARGS_QUANTITY = 3;
  private static final int QUANTITY_ARG_INDEX = 2;

  private static final int INVALID_QUANTITY = -1;
  private static final int DEFAULT_QUANTITY = 1;

  protected final StockProcessor stockProcessor;
  protected final Messages messages;

  public abstract void sendTransactionMessage(Player player);

  @Override
  public void onCommand(Player player, String[] args) {
    int quantity = determineQuantity(args);
    if (quantity == INVALID_QUANTITY) {
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

  private int determineQuantity(String[] args) {
    if (args.length != MIN_ARGS_QUANTITY) {
      return DEFAULT_QUANTITY;
    }

    Integer quantity = getInteger(args[QUANTITY_ARG_INDEX]);
    if (quantity == null || quantity <= 0) {
      return INVALID_QUANTITY;
    }

    return quantity;
  }

  private Integer getInteger(String string) {
    try {
      return Integer.parseInt(string);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
