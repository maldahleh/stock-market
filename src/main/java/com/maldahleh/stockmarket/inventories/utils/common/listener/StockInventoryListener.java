package com.maldahleh.stockmarket.inventories.utils.common.listener;

import com.maldahleh.stockmarket.inventories.utils.common.StockInventory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public record StockInventoryListener(StockInventory inventory) implements Listener {

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (inventory.isNotViewing(e.getWhoClicked())) {
      return;
    }

    e.setCancelled(true);
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
