package com.maldahleh.stockmarket.transactions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maldahleh.stockmarket.transactions.types.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionTests {

  @Test
  void testPurchase() {
    // GIVEN
    UUID uuid = UUID.randomUUID();
    String symbol = "BA";
    int quantity = 2;
    BigDecimal price = BigDecimal.ONE;
    BigDecimal fees = BigDecimal.TEN;
    BigDecimal grandTotal = BigDecimal.valueOf(11);

    // WHEN
    Transaction transaction = Transaction.buildPurchase(
        uuid,
        symbol,
        quantity,
        price,
        fees,
        grandTotal
    );

    // THEN
    assertFalse(transaction.isSold());
    assertEquals(uuid, transaction.getUuid());
    assertEquals(symbol, transaction.getSymbol());
    assertEquals(quantity, transaction.getQuantity());
    assertEquals(price, transaction.getSinglePrice());
    assertEquals(fees, transaction.getBrokerFee());
    assertEquals(grandTotal, transaction.getGrandTotal());
    assertEquals(BigDecimal.valueOf(2), transaction.getStockValue());
    assertEquals(TransactionType.PURCHASE, transaction.getTransactionType());
    assertNull(transaction.getEarnings());
    assertNotNull(transaction.getTransactionDate());
  }

  @Test
  void testSale() {
    // GIVEN
    UUID uuid = UUID.randomUUID();
    String symbol = "BA";
    int quantity = 2;
    BigDecimal price = BigDecimal.ONE;
    BigDecimal fees = BigDecimal.TEN;
    BigDecimal net = BigDecimal.valueOf(100);
    BigDecimal grandTotal = BigDecimal.valueOf(11);

    // WHEN
    Transaction transaction = Transaction.buildSale(
        uuid,
        symbol,
        quantity,
        price,
        fees,
        net,
        grandTotal
    );

    // THEN
    assertFalse(transaction.isSold());
    assertEquals(uuid, transaction.getUuid());
    assertEquals(symbol, transaction.getSymbol());
    assertEquals(quantity, transaction.getQuantity());
    assertEquals(price, transaction.getSinglePrice());
    assertEquals(fees, transaction.getBrokerFee());
    assertEquals(grandTotal, transaction.getGrandTotal());
    assertEquals(BigDecimal.valueOf(2), transaction.getStockValue());
    assertEquals(TransactionType.SALE, transaction.getTransactionType());
    assertEquals(net, transaction.getEarnings());
    assertNotNull(transaction.getTransactionDate());
  }

  @Test
  void markSold() {
    // GIVEN
    Transaction transaction = Transaction.buildPurchase(
        UUID.randomUUID(),
        "BA",
        2,
        BigDecimal.ONE,
        BigDecimal.TEN,
        BigDecimal.valueOf(12)
    );

    // WHEN
    transaction.markSold();

    // THEN
    assertTrue(transaction.isSold());
  }

  @Test
  void idSet() {
    // GIVEN
    int id = 1;

    Transaction transaction = Transaction.buildPurchase(
        UUID.randomUUID(),
        "BA",
        2,
        BigDecimal.ONE,
        BigDecimal.TEN,
        BigDecimal.valueOf(12)
    );

    // WHEN
    transaction.setId(id);

    // THEN
    assertEquals(1, transaction.getId());
  }

  @Test
  void zeroMinutesElapsed() {
    // GIVEN
    Transaction transaction = Transaction.buildPurchase(
        UUID.randomUUID(),
        "BA",
        2,
        BigDecimal.ONE,
        BigDecimal.TEN,
        BigDecimal.valueOf(12)
    );

    // WHEN
    boolean elapsed = transaction.hasElapsed(0);

    // THEN
    assertTrue(elapsed);
  }

  @Test
  void timeHasNotElapsed() {
    // GIVEN
    Transaction transaction = Transaction.buildPurchase(
        UUID.randomUUID(),
        "BA",
        2,
        BigDecimal.ONE,
        BigDecimal.TEN,
        BigDecimal.valueOf(12)
    );

    // WHEN
    boolean elapsed = transaction.hasElapsed(5);

    // THEN
    assertFalse(elapsed);
  }

  @Test
  void timeHasElapsed() {
    // GIVEN
    Transaction transaction = new Transaction(
        0,
        UUID.randomUUID(),
        TransactionType.PURCHASE,
        Instant.now().minus(20, ChronoUnit.MINUTES),
        "BA",
        2,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.TEN,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        false
    );

    // WHEN
    boolean elapsed = transaction.hasElapsed(15);

    // THEN
    assertTrue(elapsed);
  }

  @Test
  void stockValueNotComputer() {
    // GIVEN
    Transaction transaction = new Transaction(
        0,
        UUID.randomUUID(),
        TransactionType.PURCHASE,
        Instant.now().minus(20, ChronoUnit.MINUTES),
        "BA",
        2,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.TEN,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        false
    );

    // WHEN
    BigDecimal stockValue = transaction.getStockValue();

    // THEN
    assertEquals(BigDecimal.ZERO, stockValue);
  }

  @Test
  void grandTotalComputed() {
    // GIVEN
    Transaction transaction = new Transaction(
        0,
        UUID.randomUUID(),
        TransactionType.PURCHASE,
        Instant.now().minus(20, ChronoUnit.MINUTES),
        "BA",
        2,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.TEN,
        BigDecimal.ZERO,
        null,
        false
    );

    // WHEN
    BigDecimal grandTotal = transaction.getGrandTotal();

    // THEN
    assertEquals(BigDecimal.valueOf(3), grandTotal);
  }
}
