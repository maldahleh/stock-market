package com.maldahleh.stockmarket.players.utils;

import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TransactionUtils {

  public Transaction buildTransaction(TransactionType type, int quantity, BigDecimal stockValue) {
    return Transaction.builder()
        .symbol("BA")
        .transactionType(type)
        .transactionDate(Instant.now())
        .quantity(quantity)
        .stockValue(stockValue)
        .build();
  }
}
