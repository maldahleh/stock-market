package com.maldahleh.stockmarket.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class Utils {
  public static String color(String message) {
    return ChatColor.translateAlternateColorCodes('&', message);
  }

  public static ItemStack createItemStack(ConfigurationSection configurationSection) {
    return new ItemStackBuilder(
        Material.valueOf(configurationSection.getString("material")),
        configurationSection.getInt("amount"),
        (byte) configurationSection.getInt("durability")
    )
        .setDisplayName(configurationSection.getString("name"))
        .addLore(configurationSection.getStringList("lore"))
        .buildItemStack();
  }
}
