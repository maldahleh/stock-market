package com.maldahleh.stockmarket.inventories.tutorial;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.inventories.utils.common.StockInventory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class TutorialInventory extends StockInventory {

  private final Inventory inventory;

  public TutorialInventory(StockMarket plugin, ConfigSection section) {
    super(plugin);

    this.inventory = Bukkit.createInventory(null, section.getInt("size"),
        section.getString("name"));

    for (String key : section.getSection("items").getKeys()) {
      inventory.setItem(Integer.parseInt(key), section.getItemStack("items." + key));
    }
  }

  public void openInventory(Player player) {
    player.openInventory(inventory);
    addViewer(player);
  }
}
