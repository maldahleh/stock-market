package com.maldahleh.stockmarket;

import com.maldahleh.stockmarket.trade.Wallet;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakeWallet implements Wallet {

  public final Map<UUID, BigDecimal> balances = new HashMap<>();

  public BigDecimal balance(UUID player) {
    return balances.getOrDefault(player, BigDecimal.ZERO);
  }

  @Override
  public boolean withdraw(UUID player, BigDecimal amount) {
    if (balance(player).compareTo(amount) < 0) {
      return false;
    }
    balances.put(player, balance(player).subtract(amount));
    return true;
  }

  @Override
  public boolean deposit(UUID player, BigDecimal amount) {
    balances.put(player, balance(player).add(amount));
    return true;
  }
}
