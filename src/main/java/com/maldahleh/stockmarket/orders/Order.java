package com.maldahleh.stockmarket.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A standing order, filled by the order book when the price reaches {@code target}.
 *
 * @param target server price that triggers the order
 */
public record Order(long id, UUID owner, String symbol, OrderType type, long shares,
    BigDecimal target, Instant created, Instant expires) {

  public Order withId(long newId) {
    return new Order(newId, owner, symbol, type, shares, target, created, expires);
  }
}
