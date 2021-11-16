package com.maldahleh.stockmarket.inventories.utils.paged.provider;

import java.util.Map;
import org.bukkit.inventory.ItemStack;

public interface IContentProvider<L, K, V, T, U> {

  Map<K, V> getContent(L lookup);

  Map<T, U> applyTransformations(Map<K, V> data);

  Map<String, Object> getExtraData(L lookup);

  ItemStack getContentStack(ItemStack baseStack, int position, T key, U value);

  ItemStack getExtraItem(ItemStack baseStack, Map<String, Object> extraData);
}
