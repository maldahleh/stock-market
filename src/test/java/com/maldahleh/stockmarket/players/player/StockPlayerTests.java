package com.maldahleh.stockmarket.players.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maldahleh.stockmarket.players.utils.TransactionUtils;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StockPlayerTests {

  @Test
  void performTransactions() {
    // GIVEN
    Transaction purchase = TransactionUtils.buildTransaction(TransactionType.PURCHASE, 3,
        BigDecimal.TEN);
    Transaction sale = TransactionUtils.buildTransaction(TransactionType.SALE, 2, BigDecimal.ONE);

    StockPlayer stockPlayer = new StockPlayer();

    // WHEN
    stockPlayer.addTransaction(purchase);
    stockPlayer.addTransaction(sale);

    // THEN
    assertEquals(BigDecimal.valueOf(9), stockPlayer.getPortfolioValue());

    assertEquals(2, stockPlayer.getTransactions().size());
    assertTrue(stockPlayer.getTransactions().contains(purchase));
    assertTrue(stockPlayer.getTransactions().contains(sale));

    assertEquals(2, stockPlayer.getTransactionMap().size());
    assertEquals(1, stockPlayer.getStockMap().get("BA").getQuantity());
    assertEquals(BigDecimal.valueOf(9), stockPlayer.getStockMap().get("BA").getValue());
  }
}
