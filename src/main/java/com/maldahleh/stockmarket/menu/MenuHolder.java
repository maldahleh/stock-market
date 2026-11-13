package com.maldahleh.stockmarket.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Marks an inventory as one of ours and holds what each slot does when clicked. */
public class MenuHolder implements InventoryHolder {

  private final Inventory inventory;
  private final Map<Integer, Consumer<ClickType>> actions = new HashMap<>();

  public MenuHolder(int size, String title) {
    this.inventory = Bukkit.createInventory(this, Math.max(9, Math.min(54, roundUp(size))), title);
  }

  private static int roundUp(int size) {
    return (size + 8) / 9 * 9;
  }

  public void set(int slot, ItemStack item) {
    if (slot >= 0 && slot < inventory.getSize()) {
      inventory.setItem(slot, item);
    }
  }

  public void set(int slot, ItemStack item, Consumer<ClickType> action) {
    set(slot, item);
    actions.put(slot, action);
  }

  void click(int slot, ClickType type) {
    Consumer<ClickType> action = actions.get(slot);
    if (action != null) {
      action.accept(type);
    }
  }

  @Override
  public Inventory getInventory() {
    return inventory;
  }
}
