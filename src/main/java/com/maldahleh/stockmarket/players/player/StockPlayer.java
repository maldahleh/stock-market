package com.maldahleh.stockmarket.players.player;

import com.maldahleh.stockmarket.players.player.data.StockData;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
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

  public void addTransaction(Transaction transaction) {
    String symbol = transaction.getSymbol().toUpperCase();
    StockData data = stockMap.getOrDefault(symbol, new StockData());
    if (transaction.getTransactionType() == TransactionType.PURCHASE) {
      portfolioValue = portfolioValue.add(transaction.getStockValue());
      data.increase(transaction);
    } else if (transaction.getTransactionType() == TransactionType.SALE) {
      portfolioValue = portfolioValue.subtract(transaction.getStockValue());
      data.decrease(transaction);
    }

    transactionMap.put(transaction.getTransactionDate(), transaction);
    stockMap.put(symbol, data);
  }

  public BigDecimal getProfitMargin(BigDecimal currentValue) {
    return currentValue.subtract(portfolioValue);
  }

  public Collection<Transaction> getTransactions() {
    return transactionMap.values();
  }
}
