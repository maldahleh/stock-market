package com.maldahleh.stockmarket.trade;

import com.maldahleh.stockmarket.market.Asset;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.LedgerEntry;
import java.math.BigDecimal;

public sealed interface TradeResult {

  /**
   * @param net for sales, money received minus the cost of the shares sold; null for purchases
   */
  record Success(LedgerEntry entry, Asset asset, Holding holding, BigDecimal value,
      BigDecimal fee, BigDecimal total, BigDecimal net) implements TradeResult {}

  /**
   * @param when for MARKET_CLOSED, when the market reopens; otherwise null
   * @param amount for INSUFFICIENT_FUNDS, what the trade would have cost; otherwise null
   */
  record Failure(Reason reason, java.time.Instant when, BigDecimal amount)
      implements TradeResult {

    public Failure(Reason reason) {
      this(reason, null, null);
    }

    public Failure(Reason reason, java.time.Instant when) {
      this(reason, when, null);
    }
  }

  enum Reason {
    INVALID_QUANTITY,
    TOO_MANY_SHARES,
    BUSY,
    INVALID_SYMBOL,
    NOT_TRADABLE,
    CRYPTO_DISABLED,
    MARKET_CLOSED,
    STALE_PRICE,
    PRICE_TOO_LOW,
    FEES_TOO_HIGH,
    INSUFFICIENT_FUNDS,
    INSUFFICIENT_SHARES,
    PRICE_MOVED,
    DATA_UNAVAILABLE,
    STORAGE_ERROR
  }
}
