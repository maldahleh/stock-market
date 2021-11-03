package com.maldahleh.stockmarket.stocks.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.maldahleh.stockmarket.config.Settings;
import java.util.concurrent.TimeUnit;

public abstract class CacheableProvider<V> {

  protected final Cache<String, V> cache;

  protected CacheableProvider(Settings settings) {
    this.cache = CacheBuilder.newBuilder()
        .expireAfterWrite(settings.getCacheMinutes(), TimeUnit.MINUTES)
        .maximumSize(500)
        .build();
  }

  protected abstract V fetch(String key);

  protected V get(String key) {
    String uppercaseKey = key.toUpperCase();
    V result = cache.getIfPresent(uppercaseKey);
    if (result != null) {
      return result;
    }

    V fetchedResult = fetch(uppercaseKey);
    if (fetchedResult == null) {
      return null;
    }

    cache.put(uppercaseKey, fetchedResult);
    return fetchedResult;
  }
}
