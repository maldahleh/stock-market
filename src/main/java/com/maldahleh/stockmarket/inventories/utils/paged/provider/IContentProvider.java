package com.maldahleh.stockmarket.inventories.utils.paged.provider;

import java.util.Map;
import org.bukkit.inventory.ItemStack;

public interface IContentProvider<L, K, V, T, TV> {
  Map<K, V> getContent(L lookup);

  Map<T, TV> applyTransformations(Map<K, V> data);

  Map<String, Object> getExtraData(L lookup);

  ItemStack getContentStack(ItemStack baseStack, int position, T key, TV value);

  ItemStack getExtraItem(ItemStack baseStack, Map<String, Object> extraData);
}
