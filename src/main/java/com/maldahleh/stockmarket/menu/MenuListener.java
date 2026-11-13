package com.maldahleh.stockmarket.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onClick(InventoryClickEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
      return;
    }
    event.setCancelled(true);
    if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
      holder.click(event.getRawSlot(), event.getClick());
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onDrag(InventoryDragEvent event) {
    if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
      event.setCancelled(true);
    }
  }
}
