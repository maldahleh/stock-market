package com.maldahleh.stockmarket.inventories.list.listeners;

import com.maldahleh.stockmarket.inventories.list.ListInventory;
import com.maldahleh.stockmarket.inventories.lookup.LookupInventory;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import com.maldahleh.stockmarket.processor.types.SaleProcessor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public record ListListener(ListInventory inventory, LookupInventory lookupInventory,
                           PurchaseProcessor purchaseProcessor,
                           SaleProcessor saleProcessor)
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
      case SHIFT_LEFT -> purchaseProcessor.processTransaction(player, symbol, 5);
      case LEFT -> purchaseProcessor.processTransaction(player, symbol, 1);
      case MIDDLE -> lookupInventory.openInventory(player, symbol.toUpperCase());
      case RIGHT -> saleProcessor.processTransaction(player, symbol, 1);
      case SHIFT_RIGHT -> saleProcessor.processTransaction(player, symbol, 5);
      default -> {
        // other actions not supported
      }
    }
  }
}
