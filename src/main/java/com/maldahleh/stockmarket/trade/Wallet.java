package com.maldahleh.stockmarket.trade;

import java.math.BigDecimal;
import java.util.UUID;

/** The server's economy. Only call it on the server thread. */
public interface Wallet {

  /** @return true only if the whole amount was taken */
  boolean withdraw(UUID player, BigDecimal amount);

  /** @return true only if the whole amount was paid */
  boolean deposit(UUID player, BigDecimal amount);
}
