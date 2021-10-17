package com.maldahleh.stockmarket.utils;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemStackBuilder {

  private final ItemStack is;

  public ItemStackBuilder(Material m, int amount, byte durability) {
    is = new ItemStack(m, amount, durability);
  }

  public ItemStackBuilder setDisplayName(String displayName) {
    ItemMeta im = is.getItemMeta();
    if (im == null) {
      return this;
    }

    im.setDisplayName(Utils.color(displayName));
    is.setItemMeta(im);
    return this;
  }

  public ItemStackBuilder addLore(List<String> lore) {
    lore.forEach(this::addLoreLine);
    return this;
  }

  private void addLoreLine(String line) {
    ItemMeta im = is.getItemMeta();
    if (im == null || im.getLore() == null) {
      return;
    }

    List<String> lore = im.hasLore() ? new ArrayList<>(im.getLore()) : new ArrayList<>();
    lore.add(Utils.color(line));

    im.setLore(lore);
    is.setItemMeta(im);
  }

  public ItemStack buildItemStack() {
    return is;
  }
}
