package com.maldahleh.stockmarket.brokers.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BrokerListenerTests {

  private NPC npc;
  private Player player;

  private BrokerManager brokerManager;
  private InventoryManager inventoryManager;
  private BrokerListener brokerListener;

  @BeforeEach
  void setup() {
    npc = mock(NPC.class);
    player = mock(Player.class);

    brokerManager = mock(BrokerManager.class);
    inventoryManager = mock(InventoryManager.class);
    brokerListener = new BrokerListener(brokerManager, inventoryManager);
  }

  @Nested
  class LeftClick {

    @Test
    void nullPlayer() {
      // GIVEN
      NPCLeftClickEvent npcLeftClickEvent = new NPCLeftClickEvent(npc, null);

      // WHEN
      brokerListener.onLeftClick(npcLeftClickEvent);

      // THEN
      verify(inventoryManager, never())
          .openListInventory(any(Player.class));
    }

    @Test
    void notBroker() {
      // GIVEN
      NPCLeftClickEvent npcLeftClickEvent = new NPCLeftClickEvent(npc, player);

      when(brokerManager.isBroker(npc))
          .thenReturn(false);

      // WHEN
      brokerListener.onLeftClick(npcLeftClickEvent);

      // THEN
      verify(inventoryManager, never())
          .openListInventory(any(Player.class));
    }

    @Test
    void isBroker() {
      // GIVEN
      NPCLeftClickEvent npcLeftClickEvent = new NPCLeftClickEvent(npc, player);

      when(brokerManager.isBroker(npc))
          .thenReturn(true);

      // WHEN
      brokerListener.onLeftClick(npcLeftClickEvent);

      // THEN
      verify(inventoryManager, times(1))
          .openListInventory(player);
    }
  }

  @Nested
  class RightClick {

    @Test
    void nullPlayer() {
      // GIVEN
      NPCRightClickEvent npcRightClickEvent = new NPCRightClickEvent(npc, null);

      // WHEN
      brokerListener.onRightClick(npcRightClickEvent);

      // THEN
      verify(inventoryManager, never())
          .openListInventory(any(Player.class));
    }

    @Test
    void notBroker() {
      // GIVEN
      NPCRightClickEvent npcRightClickEvent = new NPCRightClickEvent(npc, player);

      when(brokerManager.isBroker(npc))
          .thenReturn(false);

      // WHEN
      brokerListener.onRightClick(npcRightClickEvent);

      // THEN
      verify(inventoryManager, never())
          .openListInventory(any(Player.class));
    }

    @Test
    void isBroker() {
      // GIVEN
      NPCRightClickEvent npcRightClickEvent = new NPCRightClickEvent(npc, player);

      when(brokerManager.isBroker(npc))
          .thenReturn(true);

      // WHEN
      brokerListener.onRightClick(npcRightClickEvent);

      // THEN
      verify(inventoryManager, times(1))
          .openListInventory(player);
    }
  }
}
