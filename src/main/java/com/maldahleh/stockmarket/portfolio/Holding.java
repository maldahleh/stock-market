package com.maldahleh.stockmarket.portfolio;

import com.maldahleh.stockmarket.market.Quantities;
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

  /** Cost per share, or per whole coin for crypto. */
  public BigDecimal averageCost() {
    return shares == 0 ? BigDecimal.ZERO
        : costBasis.divide(Quantities.amount(symbol, shares), 4, java.math.RoundingMode.HALF_UP);
  }
}
