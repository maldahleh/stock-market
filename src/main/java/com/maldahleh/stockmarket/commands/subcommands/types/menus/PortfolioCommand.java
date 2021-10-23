package com.maldahleh.stockmarket.commands.subcommands.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.common.TargetableCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PortfolioCommand extends TargetableCommand {

  public PortfolioCommand(Plugin plugin, InventoryManager inventoryManager, Messages messages) {
    super(plugin, inventoryManager, messages);
  }

  @Override
  public void callerAction(Player caller) {
    messages.sendPendingPortfolio(caller);
    inventoryManager.openPortfolioInventory(caller);
  }

  @Override
  public void targetAction(Player caller, UUID target) {
    messages.sendPendingPortfolioOther(caller);
    inventoryManager.openPortfolioInventory(caller, target);
  }

  @Override
  public String commandName() {
    return "portfolio";
  }

  @Override
  public String requiredPerm() {
    return "stockmarket.portfolio";
  }
}
