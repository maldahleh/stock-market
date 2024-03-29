package com.maldahleh.stockmarket.transactions;

import com.maldahleh.stockmarket.transactions.types.TransactionType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class Transaction {

  @Setter
  private Integer id;
  private final UUID uuid;
  private final TransactionType type;
  @Builder.Default
  private final Instant date = Instant.now();
  private final String symbol;
  private final int quantity;
  private final BigDecimal singlePrice;
  private final BigDecimal brokerFee;
  private final BigDecimal earnings;

  private BigDecimal stockValue;
  private BigDecimal grandTotal;

  @Setter
  @Builder.Default
  private boolean sold = false;

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
    if (minutes <= 0) {
      return true;
    }

    long elapsedMinutes = Duration.between(date, Instant.now()).toMinutes();
    return elapsedMinutes >= minutes;
  }
}
