package com.maldahleh.stockmarket.market;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A price snapshot for one symbol, in the market's currency (USD for every supported feed).
 *
 * @param tradeTime when {@code price} last traded; used to reject stale prices
 */
public record Quote(
    String symbol,
    AssetClass assetClass,
    BigDecimal price,
    Instant tradeTime,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal previousClose,
    long volume) {

  public BigDecimal changeFromClose() {
    if (previousClose == null || previousClose.signum() == 0) {
      return null;
    }
    return price.subtract(previousClose)
        .divide(previousClose, 8, java.math.RoundingMode.HALF_UP)
        .movePointRight(2);
  }
}
