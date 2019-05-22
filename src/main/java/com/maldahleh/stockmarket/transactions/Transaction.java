package com.maldahleh.stockmarket.transactions;

import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Transaction {
  private final UUID uuid;
  private final String transactionType;
  private final Date transactionDate;
  private final String symbol;
  private final int quantity;
  private final double singlePrice;
  private final double brokerFee;
  private final Double earnings;
}