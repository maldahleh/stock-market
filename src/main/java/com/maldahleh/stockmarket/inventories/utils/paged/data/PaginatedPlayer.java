package com.maldahleh.stockmarket.inventories.utils.paged.data;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.Inventory;

public class PaginatedPlayer {
  private final Map<Integer, Inventory> pageMap = new HashMap<>();
  private int currentPage = 1;

  public void addInventory(int page, Inventory inventory) {
    pageMap.put(page, inventory);
  }

  public Inventory getPreviousPage() {
    currentPage--;
    return pageMap.get(currentPage);
  }

  public Inventory getNextPage() {
    currentPage++;
    return pageMap.get(currentPage);
  }
}
