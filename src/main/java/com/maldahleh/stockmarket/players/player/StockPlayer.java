package com.maldahleh.stockmarket.players.player;

import com.maldahleh.stockmarket.transactions.Transaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class StockPlayer {
  private final NavigableMap<String, Integer> stockMap = new TreeMap<>();
  private final NavigableMap<Date, Transaction> transactionMap = new TreeMap<>();

  private BigDecimal portfolioValue = BigDecimal.ZERO;

  public void addTransaction(Transaction transaction) {
    portfolioValue = portfolioValue.add(transaction.getGrandTotal());
    transactionMap.put(transaction.getTransactionDate(), transaction);
    stockMap.put(transaction.getSymbol().toUpperCase(),
        stockMap.getOrDefault(transaction.getSymbol().toUpperCase(), 0) + 1);
  }

  public List<Map.Entry<String, Integer>> getPortfolio() {
    return new ArrayList<>(stockMap.entrySet());
  }

  public BigDecimal getPortfolioValue() {
    return portfolioValue;
  }
}
