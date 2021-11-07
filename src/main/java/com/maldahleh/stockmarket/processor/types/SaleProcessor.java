package com.maldahleh.stockmarket.processor.types;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.events.StockSaleEvent;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.processor.StockProcessor;
import com.maldahleh.stockmarket.processor.model.ProcessorContext;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.event.Event;

public class SaleProcessor extends StockProcessor {

  public SaleProcessor(StockMarket stockMarket,
      StockManager stockManager,
      PlayerManager playerManager,
      Storage storage,
      Settings settings,
      Messages messages) {
    super(stockMarket, stockManager, playerManager, storage, settings, messages);
  }

  @Override
  protected boolean shouldBlockStockPlayer(ProcessorContext context) {
    Collection<Transaction> transactions = context.getStockPlayer().getTransactions();
    if (transactions == null || transactions.isEmpty()) {
      return true;
    }

    Collection<Transaction> filteredTransactions = getValidTransactions(transactions,
        context.getSymbol());
    if (filteredTransactions.isEmpty()) {
      return true;
    }

    return hasValidQuantity(context, filteredTransactions);
  }

  @Override
  protected void calculateTotals(ProcessorContext context) {
    BigDecimal grandTotal = context.getQuantityPrice()
        .subtract(context.getBrokerFees());
    context.setGrandTotal(grandTotal);

    BigDecimal soldValue = getSoldValue(context);
    BigDecimal net = context.getQuantityPrice().subtract(soldValue);
    context.setSoldValue(soldValue);
    context.setNet(net);
  }

  @Override
  protected boolean hasInsufficientFunds(ProcessorContext context) {
    return false;
  }

  @Override
  protected Transaction buildTransaction(ProcessorContext context) {
    return Transaction.buildSale(
        context.getPlayer().getUniqueId(),
        context.getStock().getSymbol(),
        context.getQuantity(),
        context.getServerPrice(),
        context.getBrokerFees(),
        context.getNet(),
        context.getGrandTotal()
    );
  }

  @Override
  protected void processVault(ProcessorContext context) {
    stockMarket.getEcon().depositPlayer(context.getPlayer(), context.getGrandTotal().doubleValue());
  }

  @Override
  protected Event buildEvent(ProcessorContext context) {
    return new StockSaleEvent(
        context.getPlayer(),
        context.getSymbol(),
        context.getQuantity(),
        context.getServerPrice(),
        context.getBrokerFees(),
        context.getGrandTotal(),
        context.getSoldValue(),
        context.getNet()
    );
  }

  @Override
  protected void sendMessage(ProcessorContext context) {
    messages.sendSoldStockMessage(context.getPlayer(), context.getStock().getName(),
        context.getTransaction());
  }

  private BigDecimal getSoldValue(ProcessorContext context) {
    BigDecimal soldValue = BigDecimal.ZERO;
    for (Transaction sold : context.getProcessedTransactions()) {
      soldValue = soldValue.add(sold.getStockValue());
      sold.markSold();
      storage.markSold(sold);
    }

    return soldValue;
  }

  private List<Transaction> getValidTransactions(Collection<Transaction> transactions,
      String symbol) {
    return transactions.stream()
        .filter(transaction -> isValidTransaction(transaction, symbol))
        .toList();
  }

  private boolean hasValidQuantity(ProcessorContext context, Collection<Transaction> transactions) {
    int soldQuantity = 0;
    List<Transaction> transactionList = new ArrayList<>();
    for (Transaction transaction : transactions) {
      if (soldQuantity + transaction.getQuantity() > context.getQuantity()) {
        continue;
      }

      soldQuantity += transaction.getQuantity();
      transactionList.add(transaction);
      if (soldQuantity == context.getQuantity()) {
        break;
      }
    }

    context.setProcessedTransactions(transactionList);
    return soldQuantity == context.getQuantity();
  }

  private boolean isValidTransaction(Transaction transaction, String symbol) {
    return transaction.getSymbol().equalsIgnoreCase(symbol)
        || !transaction.isSold()
        || transaction.getTransactionType() == TransactionType.PURCHASE
        || transaction.hasElapsed(settings.getMinutesBetweenSale());
  }
}
