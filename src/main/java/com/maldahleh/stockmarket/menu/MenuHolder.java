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
  private Runnable refresh;

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

  /** Reopens this menu with current data, e.g. after a trade fills. */
  public void onRefresh(Runnable refresh) {
    this.refresh = refresh;
  }

  /** Reopens the menu if it knows how to; returns false otherwise. */
  public boolean refresh() {
    if (refresh == null) {
      return false;
    }
    refresh.run();
    return true;
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
