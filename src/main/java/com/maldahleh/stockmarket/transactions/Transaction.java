package com.maldahleh.stockmarket.transactions;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Transaction {
  private final UUID uuid;
  private final String transactionType;
  private final Date transactionDate;
  private final String symbol;
  private final int quantity;
  private final BigDecimal singlePrice;
  private final BigDecimal brokerFee;
  private final BigDecimal earnings;

  private BigDecimal grandTotal;

  public BigDecimal getGrandTotal() {
    if (grandTotal != null) {
      return grandTotal;
    }

    grandTotal = singlePrice.multiply(BigDecimal.valueOf(quantity)).add(brokerFee);
    return grandTotal;
  }
}