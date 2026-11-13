package com.maldahleh.stockmarket.market;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts between what players type and see (shares, or an amount of a coin such as 0.015 BTC)
 * and the whole-number units the plugin stores. Stocks are counted in whole shares. Crypto is
 * counted in hundred-millionths of a coin (Bitcoin's smallest unit), so fractions can be traded
 * while holdings, orders and the ledger keep using whole numbers.
 */
public final class Quantities {

  public static final int CRYPTO_DECIMALS = 8;

  private static final BigDecimal CRYPTO_UNIT = BigDecimal.ONE.movePointLeft(CRYPTO_DECIMALS);

  private Quantities() {}

  /** How much of the asset one stored unit is: 1 share, or 0.00000001 of a coin. */
  public static BigDecimal unitSize(String symbol) {
    return AssetClass.of(symbol) == AssetClass.CRYPTO ? CRYPTO_UNIT : BigDecimal.ONE;
  }

  /** The amount of the asset that {@code units} stored units make. */
  public static BigDecimal amount(String symbol, long units) {
    return BigDecimal.valueOf(units).multiply(unitSize(symbol)).stripTrailingZeros();
  }

  /** What {@code units} are worth at {@code price} per share or coin. */
  public static BigDecimal value(String symbol, BigDecimal price, long units) {
    return price.multiply(amount(symbol, units));
  }

  /**
   * Reads an amount a player typed, e.g. "5" shares or "0.015" BTC.
   *
   * @throws NumberFormatException if it isn't a positive amount the asset can be split into
   */
  public static long parse(String symbol, String text) {
    BigDecimal amount = new BigDecimal(text.trim().replace(",", ""));
    if (amount.signum() <= 0) {
      throw new NumberFormatException("Amount must be positive: " + text);
    }
    try {
      return amount.divide(unitSize(symbol)).setScale(0, RoundingMode.UNNECESSARY)
          .longValueExact();
    } catch (ArithmeticException e) {
      throw new NumberFormatException("Too precise or too large: " + text);
    }
  }

  /** Units for a whole-number count of shares or coins, e.g. a menu's "buy 5". */
  public static long of(String symbol, BigDecimal amount) {
    return parse(symbol, amount.toPlainString());
  }
}
