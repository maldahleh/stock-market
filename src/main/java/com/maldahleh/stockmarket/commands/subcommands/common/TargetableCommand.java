package com.maldahleh.stockmarket.commands.subcommands.common;

import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.commands.util.CommandUtils;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@RequiredArgsConstructor
public abstract class TargetableCommand extends BaseCommand {

  private final Plugin plugin;
  protected final InventoryManager inventoryManager;
  protected final Messages messages;

  public abstract void callerAction(Player caller);

  public abstract void targetAction(Player caller, UUID target);

  @Override
  public void onCommand(Player player, String[] args) {
    if (args.length == 1) {
      callerAction(player);
    } else {
      handleTargetedPlayer(player, args[1]);
    }
  }

  @Override
  public int maxArgs() {
    return 2;
  }

  @Override
  public List<String> commandHelpKeys(Player player) {
    List<String> keys = new ArrayList<>(List.of(requiredPerm()));
    if (player.hasPermission(CommandUtils.buildOtherPermission(requiredPerm()))) {
      keys.add(requiredPerm() + "-other");
    }

    return keys;
  }

  @SuppressWarnings("deprecation")
  private void handleTargetedPlayer(Player executor, String target) {
    if (!executor.hasPermission(CommandUtils.buildOtherPermission(requiredPerm()))) {
      messages.sendNoPermission(executor);
      return;
    }

    Player targetPlayer = Bukkit.getPlayer(target);
    if (targetPlayer != null) {
      targetAction(executor, targetPlayer.getUniqueId());
      return;
    }

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(target);

              targetAction(executor, offlinePlayer.getUniqueId());
            });
  }
}
