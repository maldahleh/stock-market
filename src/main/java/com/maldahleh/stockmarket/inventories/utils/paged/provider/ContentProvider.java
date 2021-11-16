package com.maldahleh.stockmarket.inventories.utils.paged.provider;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Settings;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public abstract class ContentProvider<L, K, V, T, TV>
    implements IContentProvider<L, K, V, T, TV> {

  protected final StockMarket stockMarket;
  protected final Settings settings;

  @Override
  public Map<K, V> getContent(L lookup) {
    return Collections.emptyMap();
  }

  @Override
  public Map<T, TV> applyTransformations(Map<K, V> data) {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Object> getExtraData(L lookup) {
    return Collections.emptyMap();
  }

  @Override
  public ItemStack getContentStack(ItemStack baseStack, int position, T key, TV value) {
    return null;
  }

  @Override
  public ItemStack getExtraItem(ItemStack baseStack, Map<String, Object> extraData) {
    return baseStack;
  }

  protected String formatInstant(Instant instant) {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(settings.getLocale())
        .withZone(ZoneId.systemDefault())
        .format(instant);
  }
}
