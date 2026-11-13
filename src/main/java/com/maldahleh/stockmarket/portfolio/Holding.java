package com.maldahleh.stockmarket.portfolio;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Shares one player owns of one symbol.
 *
 * @param costBasis total server money paid for these shares, fees included
 */
public record Holding(UUID owner, String symbol, long shares, BigDecimal costBasis) {

  public Holding {
    if (shares < 0) {
      throw new IllegalArgumentException("shares can't be negative: " + shares);
    }
  }

  public BigDecimal averageCost() {
    return shares == 0 ? BigDecimal.ZERO
        : costBasis.divide(BigDecimal.valueOf(shares), 4, java.math.RoundingMode.HALF_UP);
  }
}
