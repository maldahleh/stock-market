package com.maldahleh.stockmarket.inventories.list.listeners;

import com.maldahleh.stockmarket.inventories.list.ListInventory;
import com.maldahleh.stockmarket.inventories.lookup.LookupInventory;
import com.maldahleh.stockmarket.processor.StockProcessor;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AllArgsConstructor
public record ListListener(ListInventory inventory,
                           LookupInventory lookupInventory,
                           StockProcessor stockProcessor) implements
    Listener {

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player player)
        || inventory.isNotViewing(e.getWhoClicked())) {
      return;
    }

    e.setCancelled(true);
    String symbol = inventory.getSymbol(e.getRawSlot());
    if (symbol == null) {
      return;
    }

    switch (e.getClick()) {
      case SHIFT_LEFT -> stockProcessor.buyStock(player, symbol, 5);
      case LEFT -> stockProcessor.buyStock(player, symbol, 1);
      case MIDDLE -> lookupInventory.openInventory(player, symbol.toUpperCase());
      case RIGHT -> stockProcessor.sellStock(player, symbol, 1);
      case SHIFT_RIGHT -> stockProcessor.sellStock(player, symbol, 5);
    }
  }

  @EventHandler
  public void onDrag(InventoryDragEvent e) {
    if (inventory.isNotViewing(e.getWhoClicked())) {
      return;
    }

    e.setCancelled(true);
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    inventory.remove(e.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    inventory.remove(e.getPlayer());
  }
}
