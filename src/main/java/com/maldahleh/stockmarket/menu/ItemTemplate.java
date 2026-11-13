package com.maldahleh.stockmarket.menu;

import com.maldahleh.stockmarket.bukkit.Text;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * An item read from a menu file.
 *
 * @param symbol set on list items; clicking them trades that symbol
 * @param trade set on lookup items that trade the looked-up symbol when clicked
 */
public record ItemTemplate(Material material, String name, List<String> lore, String symbol,
    boolean trade) {

  public static ItemTemplate load(ConfigurationSection section) {
    Material material = Material.matchMaterial(section.getString("material", "BOOK"));
    return new ItemTemplate(material == null ? Material.BOOK : material,
        section.getString("name", ""), section.getStringList("lore"),
        section.getString("symbol"), section.getBoolean("trade"));
  }

  public ItemStack build(Map<String, String> placeholders) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(Text.render(name, placeholders));
      meta.setLore(Text.render(lore, placeholders));
      meta.addItemFlags(ItemFlag.values());
      item.setItemMeta(meta);
    }
    return item;
  }
}
