package com.maldahleh.stockmarket.utils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
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

  public String getCurrentTime() {
    SimpleDateFormat dateFormat = new SimpleDateFormat();
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    return dateFormat.format(new Date());
  }

  public Integer getInteger(String string) {
    try {
      return Integer.parseInt(string);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public String format(BigDecimal input, String unknown, Locale locale) {
    if (input == null) {
      return unknown;
    }

    return formatCurrency(input.doubleValue(), locale);
  }

  public String formatSigFig(Long input, String unknown) {
    if (input == null) {
      return unknown;
    }

    return sigFigNumber(input);
  }

  public String formatSingle(BigDecimal input, String unknown, Locale locale) {
    if (input == null) {
      return unknown;
    }

    return singleDecimal(input.doubleValue(), locale);
  }

  public String sigFigNumber(double input) {
    String suffixes = "kmbt";
    if (input < 1000) {
      return String.valueOf(input).replace(".0", "");
    }

    int exponent = (int) (Math.log(input) / Math.log(1000));
    return String.format("%.1f%c", input / Math.pow(1000, exponent), suffixes.charAt(exponent - 1));
  }

  public String formatDate(Date date, Locale locale) {
    DateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", locale);
    return dateFormat.format(date);
  }

  public String formatInstant(Instant instant, Locale locale) {
    DateTimeFormatter formatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault());
    return formatter.format(instant);
  }

  public String formatCurrency(double input, Locale locale) {
    NumberFormat decimalFormat =
        new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(locale));
    return decimalFormat.format(input);
  }

  private String singleDecimal(double input, Locale locale) {
    NumberFormat decimalFormat = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(locale));
    return decimalFormat.format(input);
  }
}
