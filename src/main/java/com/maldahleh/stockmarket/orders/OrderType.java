package com.maldahleh.stockmarket.orders;

import java.math.BigDecimal;

public enum OrderType {
  /** Buy once the server price is at or below the target. */
  LIMIT_BUY(true),
  /** Sell once the server price is at or above the target. */
  LIMIT_SELL(false),
  /** Sell at the market once the server price falls to or below the target. */
  STOP_SELL(false);

  private final boolean buying;

  OrderType(boolean buying) {
    this.buying = buying;
  }

  public boolean buying() {
    return buying;
  }

  public boolean triggeredBy(BigDecimal serverPrice, BigDecimal target) {
    int comparison = serverPrice.compareTo(target);
    return this == LIMIT_SELL ? comparison >= 0 : comparison <= 0;
  }
}
