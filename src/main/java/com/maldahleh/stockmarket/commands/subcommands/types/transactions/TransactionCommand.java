package com.maldahleh.stockmarket.commands.subcommands.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.commands.util.CommandUtils;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public abstract class TransactionCommand implements Subcommand {

  protected final StockProcessor stockProcessor;
  protected final Messages messages;

  public abstract void processTransaction(Player player, String symbol, int quantity);

  @Override
  public void onCommand(Player player, String[] args) {
    int quantity = CommandUtils.determineQuantity(args);
    if (quantity == CommandUtils.INVALID_QUANTITY) {
      messages.sendInvalidQuantity(player);
      return;
    }

    processTransaction(player, args[1], quantity);
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
  public String commandName() {
    return "transaction";
  }

  @Override
  public String requiredPerm() {
    return null;
  }
}
