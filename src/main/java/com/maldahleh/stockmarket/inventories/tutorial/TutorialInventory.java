package com.maldahleh.stockmarket.inventories.tutorial;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.inventories.utils.common.StockInventory;
import com.maldahleh.stockmarket.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class TutorialInventory extends StockInventory {

  private final Inventory bukkitInventory;

  public TutorialInventory(StockMarket plugin, ConfigurationSection section) {
    super(plugin);

    this.bukkitInventory =
        Bukkit.createInventory(
            null,
            section.getInt("inventory.size"),
            Utils.color(section.getString("inventory.name")));

    for (String key : section.getConfigurationSection("items").getKeys(false)) {
      bukkitInventory.setItem(
          Integer.parseInt(key),
          Utils.createItemStack(section.getConfigurationSection("items." + key)));
    }
  }

  public void openInventory(Player player) {
    player.openInventory(bukkitInventory);
    addViewer(player);
  }
}
