package com.maldahleh.stockmarket.players.player;

import com.maldahleh.stockmarket.players.player.data.StockData;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.NavigableMap;
import java.util.TreeMap;
import lombok.Getter;

@Getter
public class StockPlayer {
  private final NavigableMap<String, StockData> stockMap = new TreeMap<>();
  private final NavigableMap<Instant, Transaction> transactionMap = new TreeMap<>();

  private BigDecimal portfolioValue = BigDecimal.ZERO;

  public void addTransaction(Transaction transaction) {
    portfolioValue = portfolioValue.add(transaction.getGrandTotal());
    transactionMap.put(transaction.getTransactionDate(), transaction);

    StockData data = stockMap.get(transaction.getSymbol().toUpperCase());
    if (data == null) {
      data = new StockData();
    }

    data.increase(transaction);
    stockMap.put(transaction.getSymbol().toUpperCase(), data);
  }
}
