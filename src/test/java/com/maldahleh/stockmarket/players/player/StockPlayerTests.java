package com.maldahleh.stockmarket.players.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StockPlayerTests {

  @Test
  void performTransactions() {
    // GIVEN
    Transaction purchase = Transaction.builder()
        .symbol("BA")
        .type(TransactionType.PURCHASE)
        .quantity(3)
        .stockValue(BigDecimal.TEN)
        .build();
    Transaction sale = Transaction.builder()
        .symbol("BA")
        .type(TransactionType.SALE)
        .quantity(2)
        .stockValue(BigDecimal.ONE)
        .build();

    StockPlayer stockPlayer = new StockPlayer();
    BigDecimal currentValue = BigDecimal.TEN;

    // WHEN
    stockPlayer.addTransaction(purchase);
    stockPlayer.addTransaction(sale);

    BigDecimal profitMargin = stockPlayer.getProfitMargin(currentValue);

    // THEN
    assertEquals(BigDecimal.valueOf(9), stockPlayer.getPortfolioValue());
    assertEquals(BigDecimal.valueOf(1), profitMargin);

    assertEquals(2, stockPlayer.getTransactions().size());
    assertTrue(stockPlayer.getTransactions().contains(purchase));
    assertTrue(stockPlayer.getTransactions().contains(sale));

    assertEquals(2, stockPlayer.getTransactionMap().size());
    assertEquals(1, stockPlayer.getStockMap().get("BA").getQuantity());
    assertEquals(BigDecimal.valueOf(9), stockPlayer.getStockMap().get("BA").getValue());
  }
}
