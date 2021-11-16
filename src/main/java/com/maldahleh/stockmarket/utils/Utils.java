package com.maldahleh.stockmarket.utils;

import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@UtilityClass
public class Utils {

  public String color(String message) {
    return ChatColor.translateAlternateColorCodes('&', message);
  }

  public ItemStack createItemStack(ConfigurationSection configurationSection) {
    return new ItemStackBuilder(
            Material.valueOf(configurationSection.getString("material")),
            configurationSection.getInt("amount"),
            (byte) configurationSection.getInt("durability"))
        .setDisplayName(configurationSection.getString("name"))
        .addLore(configurationSection.getStringList("lore"))
        .buildItemStack();
  }

  public ItemStack createItemStack(
      ConfigurationSection section, Map<String, Object> replacementMap) {
    return updateItemStack(createItemStack(section), replacementMap);
  }

  public ItemStack updateItemStack(ItemStack stack, Map<String, Object> replacementMap) {
    if (stack == null || !stack.hasItemMeta()) {
      return stack;
    }

    ItemMeta meta = stack.getItemMeta();
    if (meta == null || !meta.hasLore()) {
      return stack;
    }

    if (meta.hasDisplayName()) {
      String displayName = meta.getDisplayName();
      for (Map.Entry<String, Object> e : replacementMap.entrySet()) {
        displayName = displayName.replace(e.getKey(), String.valueOf(e.getValue()));
      }

      meta.setDisplayName(displayName);
    }

    List<String> lore = meta.getLore();
    if (lore != null) {
      for (int index = 0; index < lore.size(); index++) {
        String loreLine = lore.get(index);
        for (Map.Entry<String, Object> e : replacementMap.entrySet()) {
          loreLine = loreLine.replace(e.getKey(), String.valueOf(e.getValue()));
        }

        lore.set(index, loreLine);
      }
    }

    meta.setLore(lore);
    stack.setItemMeta(meta);
    return stack;
  }
}
