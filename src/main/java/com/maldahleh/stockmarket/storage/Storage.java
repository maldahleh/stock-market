package com.maldahleh.stockmarket.storage;

import com.maldahleh.stockmarket.transactions.Transaction;
import java.util.List;
import java.util.UUID;

public interface Storage {
  void processPurchase(UUID uuid, Transaction transaction);

  void processSale(UUID uuid, String symbol, int amount, double singlePrice, double brokerFee,
      double net);

  List<Transaction> getPlayerTransactions(UUID uuid);

  List<Transaction> getStockTransactions(String symbol);
}
