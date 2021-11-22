package com.maldahleh.stockmarket.transactions;

import com.maldahleh.stockmarket.transactions.types.TransactionType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@AllArgsConstructor
public class Transaction {

  @Setter
  private Integer id;
  private final UUID uuid;
  private final TransactionType type;
  @Builder.Default
  private final Instant transactionDate = Instant.now();
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

    long elapsedMinutes = Duration.between(transactionDate, Instant.now()).toMinutes();
    return elapsedMinutes >= minutes;
  }

  public static Transaction buildPurchase(UUID uuid, String symbol, int quantity, BigDecimal price,
      BigDecimal brokerFees, BigDecimal grandTotal) {
    return Transaction.builder()
        .uuid(uuid)
        .type(TransactionType.PURCHASE)
        .transactionDate(Instant.now())
        .symbol(symbol)
        .quantity(quantity)
        .singlePrice(price)
        .brokerFee(brokerFees)
        .grandTotal(grandTotal)
        .sold(false)
        .build();
  }

  public static Transaction buildSale(UUID uuid, String symbol, int quantity, BigDecimal price,
      BigDecimal brokerFees, BigDecimal net, BigDecimal grandTotal) {
    return Transaction.builder()
        .uuid(uuid)
        .type(TransactionType.SALE)
        .transactionDate(Instant.now())
        .symbol(symbol)
        .quantity(quantity)
        .singlePrice(price)
        .brokerFee(brokerFees)
        .grandTotal(grandTotal)
        .earnings(net)
        .sold(false)
        .build();
  }
}
