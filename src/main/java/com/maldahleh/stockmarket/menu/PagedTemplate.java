package com.maldahleh.stockmarket.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/** A menu file with one base item repeated over content slots, and page arrows. */
public record PagedTemplate(String name, int size, ItemTemplate baseItem,
    List<Integer> contentSlots, int previousSlot, ItemTemplate previous, ItemTemplate noPrevious,
    int nextSlot, ItemTemplate next, ItemTemplate noNext, Map<Integer, ItemTemplate> extraItems) {

  public static PagedTemplate load(ConfigurationSection yaml) {
    return new PagedTemplate(yaml.getString("name", ""), yaml.getInt("size", 45),
        ItemTemplate.load(yaml.getConfigurationSection("base-item")),
        yaml.getIntegerList("content-slots"),
        yaml.getInt("previous-page.slot"),
        ItemTemplate.load(yaml.getConfigurationSection("previous-page.previous-page")),
        ItemTemplate.load(yaml.getConfigurationSection("previous-page.no-previous")),
        yaml.getInt("next-page.slot"),
        ItemTemplate.load(yaml.getConfigurationSection("next-page.next-page")),
        ItemTemplate.load(yaml.getConfigurationSection("next-page.no-next")),
        extras(yaml.getConfigurationSection("extra-items")));
  }

  private static Map<Integer, ItemTemplate> extras(ConfigurationSection section) {
    Map<Integer, ItemTemplate> items = new HashMap<>();
    if (section != null) {
      for (String key : section.getKeys(false)) {
        try {
          items.put(Integer.parseInt(key), ItemTemplate.load(section.getConfigurationSection(key)));
        } catch (NumberFormatException ignored) {
          // Only numbered slots are items.
        }
      }
    }
    return items;
  }
}
