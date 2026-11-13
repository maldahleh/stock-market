package com.maldahleh.stockmarket.bukkit;

import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;

public final class Text {

  private Text() {}

  public static String color(String text) {
    return ChatColor.translateAlternateColorCodes('&', text);
  }

  public static String fill(String text, Map<String, String> placeholders) {
    String result = text;
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      result = result.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
    }
    return result;
  }

  public static String render(String text, Map<String, String> placeholders) {
    return color(fill(text, placeholders));
  }

  public static List<String> render(List<String> lines, Map<String, String> placeholders) {
    return lines.stream().map(line -> render(line, placeholders)).toList();
  }
}
