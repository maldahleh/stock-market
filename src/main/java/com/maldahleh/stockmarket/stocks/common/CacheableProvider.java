package com.maldahleh.stockmarket.stocks.common;

import com.google.common.cache.Cache;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.stocks.utils.StockUtils;

public abstract class CacheableProvider<V> {

  protected final Cache<String, V> cache;

  protected CacheableProvider(Settings settings) {
    this.cache = StockUtils.buildCache(settings.getCacheMinutes());
  }

  protected abstract V fetch(String key);

  protected V get(String key) {
    String uppercaseKey = key.toUpperCase();
    V result = cache.getIfPresent(uppercaseKey);
    if (result != null) {
      return result;
    }

    V fetchedResult = fetch(uppercaseKey);
    cache.put(uppercaseKey, fetchedResult);
    return fetchedResult;
  }
}
