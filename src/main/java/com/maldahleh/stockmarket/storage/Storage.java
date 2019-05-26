package com.maldahleh.stockmarket.storage;

import com.maldahleh.stockmarket.transactions.Transaction;
import java.util.List;
import java.util.UUID;

public interface Storage {
  void processPurchase(Transaction transaction);

  void processSale(Transaction transaction);

  void markSold(UUID uuid, Transaction transaction);

  List<Transaction> getPlayerTransactions(UUID uuid);

  List<Transaction> getStockTransactions(String symbol);
}
