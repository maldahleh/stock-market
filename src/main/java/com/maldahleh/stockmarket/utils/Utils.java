package com.maldahleh.stockmarket.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

  public static ItemStack createItemStack(ConfigurationSection section,
      Map<String, Object> replacementMap) {
    return updateItemStack(createItemStack(section), replacementMap);
  }

  public static ItemStack updateItemStack(ItemStack stack, Map<String, Object> replacementMap) {
    if (stack == null || !stack.hasItemMeta()) {
      return stack;
    }

    ItemMeta meta = stack.getItemMeta();
    if (!meta.hasLore()) {
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
    for (int index = 0; index < lore.size(); index++) {
      String loreLine = lore.get(index);
      for (Map.Entry<String, Object> e : replacementMap.entrySet()) {
        loreLine = loreLine.replace(e.getKey(), String.valueOf(e.getValue()));
      }

      lore.set(index, loreLine);
    }

    meta.setLore(lore);
    stack.setItemMeta(meta);
    return stack;
  }

  public static String getCurrentTime() {
    SimpleDateFormat dateFormat = new SimpleDateFormat();
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    return dateFormat.format(new Date());
  }

  public static Integer getInteger (String string) {
    try {
      return Integer.parseInt(string);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static String format(BigDecimal input, String unknown) {
    if (input == null) {
      return unknown;
    }

    return formatCurrency(input.doubleValue());
  }

  public static String formatSigFig(Long input, String unknown) {
    if (input == null) {
      return unknown;
    }

    return sigFigNumber(input);
  }

  public static String formatSingle(BigDecimal input, String unknown) {
    if (input == null) {
      return unknown;
    }

    return singleDecimal(input.doubleValue());
  }

  public static String sigFigNumber(double input) {
    String suffixes = "kmbt";
    if (input < 1000) {
      return String.valueOf(input).replace(".0", "");
    }

    int exponent = (int) (Math.log(input) / Math.log(1000));
    return String.format("%.1f%c", input / Math.pow(1000, exponent), suffixes.charAt(exponent - 1));
  }

  public static String formatCurrency(double input) {
    DecimalFormat decimalFormat = new DecimalFormat("#,###.00");
    return decimalFormat.format(input);
  }

  private static String singleDecimal(double input) {
    DecimalFormat decimalFormat = new DecimalFormat("0.#");
    return decimalFormat.format(input);
  }
}
