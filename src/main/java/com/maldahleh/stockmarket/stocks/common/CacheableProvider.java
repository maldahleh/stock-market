package com.maldahleh.stockmarket.stocks.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.maldahleh.stockmarket.config.Settings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

  protected Map<String, V> fetch(String[] keys) {
    Map<String, V> fetched = new HashMap<>();
    for (String key : keys) {
      fetched.put(key, get(key));
    }

    return fetched;
  }

  public V get(String key) {
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

  public Map<String, V> get(String... keys) {
    String[] upperKeys = Arrays.stream(keys)
        .map(String::toUpperCase)
        .toArray(String[]::new);

    Map<String, V> fetched = fetch(upperKeys);
    fetched.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
    fetched.forEach(cache::put);

    return fetched;
  }
}
