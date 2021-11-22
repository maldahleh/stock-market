package com.maldahleh.stockmarket.processor.types;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.events.StockPurchaseEvent;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.processor.StockProcessor;
import com.maldahleh.stockmarket.processor.model.ProcessorContext;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import org.bukkit.event.Event;

public class PurchaseProcessor extends StockProcessor {

  public PurchaseProcessor(StockMarket stockMarket,
      StockManager stockManager,
      PlayerManager playerManager,
      Storage storage,
      Settings settings,
      Messages messages) {
    super(stockMarket, stockManager, playerManager, storage, settings, messages);
  }

  @Override
  protected boolean shouldBlockStockPlayer(ProcessorContext context) {
    return false;
  }

  @Override
  protected void calculateTotals(ProcessorContext context) {
    BigDecimal grandTotal = context.getQuantityPrice()
        .add(context.getBrokerFees());
    context.setGrandTotal(grandTotal);
  }

  @Override
  protected boolean hasInsufficientFunds(ProcessorContext context) {
    return !stockMarket.getEcon().has(context.getPlayer(), context.getGrandTotal().doubleValue());
  }

  @Override
  protected Transaction buildTransaction(ProcessorContext context) {
    return Transaction.builder()
        .uuid(context.getPlayer().getUniqueId())
        .type(TransactionType.PURCHASE)
        .symbol(context.getStock().getSymbol())
        .quantity(context.getQuantity())
        .singlePrice(context.getServerPrice())
        .brokerFee(context.getBrokerFees())
        .grandTotal(context.getGrandTotal())
        .build();
  }

  @Override
  protected void processVault(ProcessorContext context) {
    stockMarket.getEcon().withdrawPlayer(context.getPlayer(),
        context.getGrandTotal().doubleValue());
  }

  @Override
  protected void sendMessage(ProcessorContext context) {
    messages.sendBoughtStockMessage(context.getPlayer(), context.getStock().getName(),
        context.getTransaction());
  }

  @Override
  protected Event buildEvent(ProcessorContext context) {
    return new StockPurchaseEvent(
        context.getPlayer(),
        context.getSymbol(),
        context.getQuantity(),
        context.getServerPrice(),
        context.getBrokerFees(),
        context.getGrandTotal()
    );
  }
}
