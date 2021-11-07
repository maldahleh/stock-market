package com.maldahleh.stockmarket.commands.subcommands.common;

import com.maldahleh.stockmarket.commands.util.CommandUtils;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import com.maldahleh.stockmarket.processor.types.SaleProcessor;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public abstract class TransactionCommand extends NoPermissionCommand {

  protected final PurchaseProcessor purchaseProcessor;
  protected final SaleProcessor saleProcessor;
  protected final Messages messages;

  public abstract void processTransaction(Player player, String symbol, int quantity);

  @Override
  public void onCommand(Player player, String[] args) {
    int quantity = CommandUtils.determineQuantity(args);
    if (quantity == CommandUtils.INVALID_QUANTITY) {
      messages.sendInvalidQuantity(player);
      return;
    }

    String symbol = args[1];
    processTransaction(player, symbol, quantity);
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
