package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.commands.util.CommandUtils;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public record TransactionsCommand(Plugin plugin, InventoryManager inventoryManager,
                                  Messages messages) implements
    Subcommand {

  @Override
  public void onCommand(Player player, String[] args) {
    if (args.length == 1) {
      openPersonalTransactions(player);
    } else {
      openTargetTransactions(player, args[1]);
    }
  }

  @Override
  public int minArgs() {
    return 1;
  }

  @Override
  public int maxArgs() {
    return 2;
  }

  @Override
  public String requiredPerm() {
    return "stockmarket.transactions";
  }

  private void openPersonalTransactions(Player player) {
    messages.sendPendingTransactions(player);
    inventoryManager.openTransactionInventory(player);
  }

  @SuppressWarnings("deprecation")
  private void openTargetTransactions(Player executor, String targetName) {
    if (!executor.hasPermission(CommandUtils.buildOtherPermission(requiredPerm()))) {
      messages.sendNoPermission(executor);
      return;
    }

    Player target = Bukkit.getPlayer(targetName);
    if (target != null) {
      messages.sendPendingTransactionsOther(executor);
      inventoryManager.openTransactionInventory(executor, target.getUniqueId());
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);

      messages.sendPendingTransactionsOther(executor);
      inventoryManager.openTransactionInventory(executor, offlinePlayer.getUniqueId());
    });
  }
}
