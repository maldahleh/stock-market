package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.commands.util.CommandUtils;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public record PortfolioCommand(Plugin plugin, InventoryManager inventoryManager,
                               Messages messages) implements
    Subcommand {

  @Override
  public void onCommand(Player player, String[] args) {
    if (args.length == 1) {
      openPersonalPortfolio(player);
    } else {
      openTargetPortfolio(player, args[1]);
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
    return "stockmarket.portfolio";
  }

  private void openPersonalPortfolio(Player player) {
    messages.sendPendingPortfolio(player);
    inventoryManager.openPortfolioInventory(player);
  }

  @SuppressWarnings("deprecation")
  private void openTargetPortfolio(Player executor, String targetName) {
    if (!executor.hasPermission(CommandUtils.buildOtherPermission(requiredPerm()))) {
      messages.sendNoPermission(executor);
      return;
    }

    Player target = Bukkit.getPlayer(targetName);
    if (target != null) {
      messages.sendPendingPortfolioOther(executor);
      inventoryManager.openPortfolioInventory(executor, target.getUniqueId());
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);

      messages.sendPendingPortfolioOther(executor);
      inventoryManager.openPortfolioInventory(executor, offlinePlayer.getUniqueId());
    });
  }
}
