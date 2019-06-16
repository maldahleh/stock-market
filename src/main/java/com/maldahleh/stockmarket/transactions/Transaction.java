package com.maldahleh.stockmarket.transactions;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class Transaction {
  private final int id;
  private final UUID uuid;
  private final String transactionType;
  private final Instant transactionDate;
  private final String symbol;
  private final int quantity;
  private final BigDecimal singlePrice;
  private final BigDecimal brokerFee;
  private final BigDecimal earnings;

  private BigDecimal stockValue;
  private BigDecimal grandTotal;
  private boolean sold;

  public BigDecimal getStockValue() {
    if (stockValue != null) {
      return stockValue;
    }

    stockValue = singlePrice.multiply(BigDecimal.valueOf(quantity));
    return stockValue;
  }

  public BigDecimal getGrandTotal() {
    if (grandTotal != null) {
      return grandTotal;
    }

    grandTotal = singlePrice.multiply(BigDecimal.valueOf(quantity)).add(brokerFee);
    return grandTotal;
  }

  public boolean hasElapsed(int minutes) {
    if (minutes == 0) {
      return true;
    }

    return Duration.between(transactionDate, Instant.now()).compareTo(Duration
        .ofMinutes(minutes)) >= 0;
  }

  public void markSold() {
    this.sold = true;
  }
}