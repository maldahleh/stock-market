package com.maldahleh.stockmarket.players.player.data;

import com.maldahleh.stockmarket.transactions.Transaction;
import java.math.BigDecimal;
import lombok.Getter;

@Getter
public class StockData {
  private int quantity = 0;
  private BigDecimal value = BigDecimal.ZERO;

  public void increase(Transaction transaction) {
    quantity += transaction.getQuantity();
    value = value.add(transaction.getStockValue());
  }

  public void decrease(Transaction transaction) {
    quantity -= transaction.getQuantity();
    value = value.subtract(transaction.getStockValue());
  }
}
