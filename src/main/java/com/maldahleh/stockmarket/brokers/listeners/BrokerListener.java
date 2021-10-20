package com.maldahleh.stockmarket.brokers.listeners;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public record BrokerListener(BrokerManager brokerManager,
                             InventoryManager inventoryManager)
    implements Listener {

  @EventHandler
  public void onLeftClick(NPCLeftClickEvent e) {
    processEvent(e.getClicker(), e.getNPC());
  }

  @EventHandler
  public void onRightClick(NPCRightClickEvent e) {
    processEvent(e.getClicker(), e.getNPC());
  }

  private void processEvent(Player clicker, NPC target) {
    if (clicker == null || !brokerManager.isBroker(target)) {
      return;
    }

    inventoryManager.openListInventory(clicker);
  }
}
