package com.maldahleh.stockmarket.inventories.list.listeners;

import com.maldahleh.stockmarket.inventories.list.ListInventory;
import com.maldahleh.stockmarket.inventories.lookup.LookupInventory;
import com.maldahleh.stockmarket.processor.StockProcessor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public record ListListener(
    ListInventory inventory, LookupInventory lookupInventory, StockProcessor stockProcessor)
    implements Listener {

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player player)
        || inventory.isNotViewing(e.getWhoClicked())) {
      return;
    }

    String symbol = inventory.getSymbol(e.getRawSlot());
    if (symbol == null) {
      return;
    }

    processClick(e.getClick(), player, symbol);
  }

  private void processClick(ClickType clickType, Player player, String symbol) {
    switch (clickType) {
      case SHIFT_LEFT -> stockProcessor.buyStock(player, symbol, 5);
      case LEFT -> stockProcessor.buyStock(player, symbol, 1);
      case MIDDLE -> lookupInventory.openInventory(player, symbol.toUpperCase());
      case RIGHT -> stockProcessor.sellStock(player, symbol, 1);
      case SHIFT_RIGHT -> stockProcessor.sellStock(player, symbol, 5);
      default -> {
        // other actions not supported
      }
    }
  }
}
