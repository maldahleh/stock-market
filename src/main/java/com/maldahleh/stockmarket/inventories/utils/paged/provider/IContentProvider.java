package com.maldahleh.stockmarket.inventories.utils.paged.provider;

import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public interface IContentProvider<K, V, T> {
  Map<K, V> getContent(UUID uuid);

  Map<T, V> applyTransformations(Map<K, V> data);

  Map<String, Object> getExtraData(UUID uuid);

  ItemStack getContentStack(ItemStack baseStack, int position, T key, V value);

  ItemStack getExtraItem(ItemStack baseStack, Map<String, Object> extraData);
}
