package com.maldahleh.stockmarket.processor.types;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.processor.StockProcessor;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import java.util.Collection;
import java.util.List;

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
  protected boolean shouldBlockStockPlayer(StockPlayer stockPlayer, String symbol, int quantity) {
    Collection<Transaction> transactions = stockPlayer.getTransactions();
    if (transactions == null || transactions.isEmpty()) {
      return true;
    }

    Collection<Transaction> filteredTransactions = getValidTransactions(transactions, symbol);
    if (filteredTransactions.isEmpty()) {
      return true;
    }

    return hasValidQuantity(filteredTransactions, quantity);
  }

  private List<Transaction> getValidTransactions(Collection<Transaction> transactions, String symbol) {
    return transactions.stream()
        .filter(transaction -> isValidTransaction(transaction, symbol))
        .toList();
  }

  private boolean hasValidQuantity(Collection<Transaction> transactions, int quantity) {
    int soldQuantity = 0;
    for (Transaction transaction : transactions) {
      if (soldQuantity + transaction.getQuantity() > quantity) {
        continue;
      }

      soldQuantity += transaction.getQuantity();
      if (soldQuantity == quantity) {
        break;
      }
    }

    return soldQuantity == quantity;
  }

  private boolean isValidTransaction(Transaction transaction, String symbol) {
    return transaction.getSymbol().equalsIgnoreCase(symbol)
        || !transaction.isSold()
        || transaction.getTransactionType() == TransactionType.PURCHASE
        || transaction.hasElapsed(settings.getMinutesBetweenSale());
  }
}
