package com.maldahleh.stockmarket.inventories.list;

import com.maldahleh.stockmarket.inventories.list.listeners.ListListener;
import com.maldahleh.stockmarket.inventories.lookup.LookupInventory;
import com.maldahleh.stockmarket.processor.StockProcessor;
import com.maldahleh.stockmarket.utils.Utils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class ListInventory {

  private final Inventory listInventory;
  private final Set<UUID> activeViewers;
  private final Map<Integer, String> symbolMap;

  public ListInventory(
      Plugin plugin,
      StockProcessor processor,
      LookupInventory lookupInventory,
      ConfigurationSection section) {
    this.activeViewers = new HashSet<>();
    this.symbolMap = new HashMap<>();
    this.listInventory =
        Bukkit.createInventory(
            null,
            section.getInt("inventory.size"),
            Utils.color(section.getString("inventory.name")));

    for (String key : section.getConfigurationSection("items").getKeys(false)) {
      Integer slot = Integer.valueOf(key);
      String symbol = section.getString("items." + key + ".symbol");
      if (!symbol.isEmpty()) {
        symbolMap.put(slot, symbol.toUpperCase());
      }

      listInventory.setItem(
          slot, Utils.createItemStack(section.getConfigurationSection("items." + key)));
    }

    Bukkit.getServer()
        .getPluginManager()
        .registerEvents(new ListListener(this, lookupInventory, processor), plugin);
  }

  public void openInventory(Player player) {
    player.openInventory(listInventory);
    activeViewers.add(player.getUniqueId());
  }

  public String getSymbol(int slot) {
    return symbolMap.get(slot);
  }

  public boolean isNotViewing(HumanEntity entity) {
    return !activeViewers.contains(entity.getUniqueId());
  }

  public void remove(HumanEntity entity) {
    activeViewers.remove(entity.getUniqueId());
  }
}
