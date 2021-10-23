package com.maldahleh.stockmarket.inventories.utils.common;

import com.maldahleh.stockmarket.inventories.utils.common.listener.StockInventoryListener;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.HumanEntity;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class StockInventory {

  private final Set<UUID> activeViewers = new HashSet<>();

  protected StockInventory(JavaPlugin plugin) {
    plugin.getServer().getPluginManager().registerEvents(new StockInventoryListener(this), plugin);
  }

  public void addViewer(HumanEntity entity) {
    activeViewers.add(entity.getUniqueId());
  }

  public boolean isNotViewing(HumanEntity entity) {
    return !activeViewers.contains(entity.getUniqueId());
  }

  public void remove(HumanEntity entity) {
    activeViewers.remove(entity.getUniqueId());
  }
}
