package com.maldahleh.stockmarket.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

@UtilityClass
public class Utils {

  public String color(String message) {
    return ChatColor.translateAlternateColorCodes('&', message);
  }

  public ItemStack createItemStack(ConfigurationSection section) {
    return createItemStack(section, Collections.emptyMap());
  }

  public ItemStack createItemStack(ConfigurationSection section, Map<String, Object> replacements) {
    ItemStack itemStack = new ItemStack(
        Material.valueOf(section.getString("material")),
        section.getInt("amount")
    );

    setDamage(itemStack, section.getInt("durability"));
    setDisplayName(itemStack, section.getString("name"), replacements);
    setLore(itemStack, section.getStringList("lore"), replacements);
    return itemStack;
  }

  public ItemStack updateItemStack(ItemStack stack, Map<String, Object> replacements) {
    ItemMeta meta = getItemMeta(stack);
    if (meta == null) {
      return stack;
    }

    if (meta.hasDisplayName()) {
      setDisplayName(stack, meta.getDisplayName(), replacements);
    }

    if (meta.hasLore()) {
      setLore(stack, meta.getLore(), replacements);
    }

    return stack;
  }

  private void setDamage(ItemStack stack, int damage) {
    ItemMeta meta = getItemMeta(stack);
    if (meta instanceof Damageable damageable) {
      damageable.setDamage(damage);
      stack.setItemMeta(meta);
    }
  }

  private void setDisplayName(ItemStack stack, String name, Map<String, Object> replacements) {
    ItemMeta meta = getItemMeta(stack);
    if (meta == null) {
      return;
    }

    String formattedLine = formatLine(name, replacements);
    meta.setDisplayName(formattedLine);

    stack.setItemMeta(meta);
  }

  private void setLore(ItemStack stack, List<String> lore, Map<String, Object> replacements) {
    ItemMeta meta = getItemMeta(stack);
    if (meta == null) {
      return;
    }

    List<String> replacedLore = lore.stream()
        .map(line -> formatLine(line, replacements))
        .toList();
    meta.setLore(replacedLore);

    stack.setItemMeta(meta);
  }

  private String formatLine(String line, Map<String, Object> replacements) {
    String colored = color(line);
    return replace(colored, replacements);
  }

  private String replace(String value, Map<String, Object> replacements) {
    for (Entry<String, Object> e : replacements.entrySet()) {
      value = value.replace(e.getKey(), String.valueOf(e.getValue()));
    }

    return value;
  }

  private ItemMeta getItemMeta(ItemStack stack) {
    if (stack == null || !stack.hasItemMeta()) {
      return null;
    }

    return stack.getItemMeta();
  }
}
