package com.maldahleh.stockmarket.inventories.list;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.inventories.list.listeners.ListListener;
import com.maldahleh.stockmarket.inventories.lookup.LookupInventory;
import com.maldahleh.stockmarket.inventories.utils.common.StockInventory;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import com.maldahleh.stockmarket.processor.types.SaleProcessor;
import com.maldahleh.stockmarket.utils.Utils;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ListInventory extends StockInventory {

  private final Inventory inventory;
  private final Map<Integer, String> symbolMap;

  public ListInventory(
      StockMarket plugin,
      PurchaseProcessor purchaseProcessor,
      SaleProcessor saleProcessor,
      LookupInventory lookupInventory,
      ConfigSection section) {
    super(plugin);

    this.symbolMap = new HashMap<>();
    this.inventory = Bukkit.createInventory(null, section.getInt("size"),
        section.getString("name"));

    for (String key : section.getSection("items").getKeys()) {
      int slot = Integer.parseInt(key);
      String symbol = section.getString("items." + key + ".symbol");
      if (symbol != null && !symbol.isEmpty()) {
        symbolMap.put(slot, symbol.toUpperCase());
      }

      inventory.setItem(slot, Utils.createItemStack(section.getSection("items." + key)));
    }

    Bukkit.getServer()
        .getPluginManager()
        .registerEvents(new ListListener(this, lookupInventory, purchaseProcessor, saleProcessor),
            plugin);
  }

  public void openInventory(Player player) {
    player.openInventory(inventory);
    addViewer(player);
  }

  public String getSymbol(int slot) {
    return symbolMap.get(slot);
  }
}
