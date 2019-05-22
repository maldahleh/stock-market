package com.maldahleh.stockmarket.inventories.tutorial.listeners;

import com.maldahleh.stockmarket.inventories.tutorial.TutorialInventory;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AllArgsConstructor
public class TutorialListener implements Listener {
  private final TutorialInventory inventory;

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!inventory.isActive(e.getWhoClicked())) {
      return;
    }

    e.setCancelled(true);
  }

  @EventHandler
  public void onDrag(InventoryDragEvent e) {
    if (!inventory.isActive(e.getWhoClicked())) {
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
