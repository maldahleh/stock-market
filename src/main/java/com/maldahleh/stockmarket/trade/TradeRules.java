package com.maldahleh.stockmarket.trade;

import com.maldahleh.stockmarket.market.Quantities;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Set;

/**
 * Server settings that decide whether a trade may happen and what it costs.
 *
 * @param priceMultiplier server money per dollar of market price
 * @param flatFee server money charged per trade
 * @param percentFee percent of the trade value charged per trade, e.g. 0.5 for 0.5%
 * @param maxQuoteAge how old the last trade may be; older prices can't be traded on
 * @param blockedExchanges exchange codes (e.g. OTC) that can't be traded
 * @param maxSharesPerTrade most shares, or coins, one trade or order may move
 */
public record TradeRules(
    BigDecimal priceMultiplier,
    BigDecimal flatFee,
    BigDecimal percentFee,
    boolean feesOnBuy,
    boolean feesOnSell,
    BigDecimal minimumPrice,
    Duration maxQuoteAge,
    boolean allowCrypto,
    Set<String> blockedExchanges,
    long maxSharesPerTrade) {

  public BigDecimal serverPrice(BigDecimal marketPrice) {
    return marketPrice.multiply(priceMultiplier).setScale(2, RoundingMode.HALF_UP);
  }

  /** Whether {@code units} is more than one trade may move. */
  public boolean tooLarge(String symbol, long units) {
    return Quantities.amount(symbol, units).compareTo(BigDecimal.valueOf(maxSharesPerTrade)) > 0;
  }

  public BigDecimal fee(BigDecimal value, boolean buying) {
    if (buying ? !feesOnBuy : !feesOnSell) {
      return BigDecimal.ZERO;
    }
    return value.multiply(percentFee).movePointLeft(2).add(flatFee)
        .setScale(2, RoundingMode.HALF_UP);
  }
}
