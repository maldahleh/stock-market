package com.maldahleh.stockmarket.players.player;

import com.maldahleh.stockmarket.players.player.data.StockData;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;
import lombok.Getter;

@Getter
public class StockPlayer {

  private final NavigableMap<String, StockData> stockMap = new TreeMap<>();
  private final NavigableMap<Instant, Transaction> transactionMap = new TreeMap<>();

  private BigDecimal portfolioValue = BigDecimal.ZERO;

  public void addPurchaseTransaction(Transaction transaction) {
    portfolioValue = portfolioValue.add(transaction.getStockValue());
    transactionMap.put(transaction.getTransactionDate(), transaction);

    StockData data = stockMap.getOrDefault(transaction.getSymbol().toUpperCase(), new StockData());
    data.increase(transaction);

    stockMap.put(transaction.getSymbol().toUpperCase(), data);
  }

  public void addSaleTransaction(Transaction transaction) {
    portfolioValue = portfolioValue.subtract(transaction.getStockValue());
    transactionMap.put(transaction.getTransactionDate(), transaction);

    StockData data = stockMap.getOrDefault(transaction.getSymbol().toUpperCase(), new StockData());
    data.decrease(transaction);

    stockMap.put(transaction.getSymbol().toUpperCase(), data);
  }

  public Collection<Transaction> getTransactions() {
    return transactionMap.values();
  }
}
