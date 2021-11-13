package com.maldahleh.stockmarket.players.player.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.maldahleh.stockmarket.transactions.Transaction;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StockDataTests {

  @Test
  void performOperations() {
    // GIVEN
    Transaction addTransaction = buildTransaction(5, BigDecimal.TEN);
    Transaction subtractTransaction = buildTransaction(3, BigDecimal.ONE);

    StockData stockData = new StockData();

    // WHEN
    stockData.increase(addTransaction);
    stockData.decrease(subtractTransaction);

    // THEN
    assertEquals(2, stockData.getQuantity());
    assertEquals(BigDecimal.valueOf(9), stockData.getValue());
  }

  private Transaction buildTransaction(int quantity, BigDecimal stockValue) {
    return Transaction.builder()
        .quantity(quantity)
        .stockValue(stockValue)
        .build();
  }
}
