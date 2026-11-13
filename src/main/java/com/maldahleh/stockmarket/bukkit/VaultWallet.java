package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.trade.Wallet;
import java.math.BigDecimal;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public record VaultWallet(Economy economy) implements Wallet {

  @Override
  public boolean withdraw(UUID player, BigDecimal amount) {
    OfflinePlayer target = Bukkit.getOfflinePlayer(player);
    double value = amount.doubleValue();
    return economy.has(target, value) && economy.withdrawPlayer(target, value).transactionSuccess();
  }

  @Override
  public boolean deposit(UUID player, BigDecimal amount) {
    return economy.depositPlayer(Bukkit.getOfflinePlayer(player), amount.doubleValue())
        .transactionSuccess();
  }
}
