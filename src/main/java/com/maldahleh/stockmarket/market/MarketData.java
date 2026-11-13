package com.maldahleh.stockmarket.market;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Caches the provider so menus and placeholders don't spend the rate limit. Trades ask for a
 * fresh price instead of a cached one. Futures complete on the network executor.
 */
public class MarketData {

  private static final Duration ASSET_TTL = Duration.ofHours(6);
  private static final Duration BARS_TTL = Duration.ofHours(1);
  private static final Duration CLOCK_TTL = Duration.ofMinutes(1);

  private final MarketDataProvider provider;
  private final Executor network;
  private final Duration quoteTtl;

  private final Map<String, Timed<Quote>> quotes = new ConcurrentHashMap<>();
  private final Map<String, Timed<Optional<Asset>>> assets = new ConcurrentHashMap<>();
  private final Map<String, Timed<List<DailyBar>>> bars = new ConcurrentHashMap<>();
  private volatile Timed<MarketClock> clock;

  public MarketData(MarketDataProvider provider, Executor network, Duration quoteTtl) {
    this.provider = provider;
    this.network = network;
    this.quoteTtl = quoteTtl;
  }

  public MarketDataProvider provider() {
    return provider;
  }

  /** The cached quote, if any, without fetching. Safe on the server thread. */
  public Optional<Quote> cachedQuote(String symbol) {
    Timed<Quote> timed = quotes.get(symbol);
    return timed == null ? Optional.empty() : Optional.of(timed.value());
  }

  /** Quotes no older than the cache time, fetching the missing ones in one call. */
  public CompletableFuture<Map<String, Quote>> quotes(Collection<String> symbols) {
    return quotes(symbols, quoteTtl);
  }

  /** A quote fetched now, for trades. */
  public CompletableFuture<Optional<Quote>> freshQuote(String symbol) {
    return quotes(Set.of(symbol), Duration.ZERO)
        .thenApply(result -> Optional.ofNullable(result.get(symbol)));
  }

  private CompletableFuture<Map<String, Quote>> quotes(Collection<String> symbols, Duration ttl) {
    Instant now = Instant.now();
    Map<String, Quote> found = new HashMap<>();
    Set<String> missing = new java.util.HashSet<>();
    for (String symbol : symbols) {
      Timed<Quote> timed = quotes.get(symbol);
      if (timed != null && !ttl.isZero() && timed.at().plus(ttl).isAfter(now)) {
        found.put(symbol, timed.value());
      } else {
        missing.add(symbol);
      }
    }
    if (missing.isEmpty()) {
      return CompletableFuture.completedFuture(found);
    }
    return call(() -> provider.quotes(missing)).thenApply(fetched -> {
      Instant at = Instant.now();
      fetched.forEach((symbol, quote) -> quotes.put(symbol, new Timed<>(quote, at)));
      found.putAll(fetched);
      return found;
    });
  }

  public CompletableFuture<Optional<Asset>> asset(String symbol) {
    Timed<Optional<Asset>> timed = assets.get(symbol);
    if (timed != null && timed.at().plus(ASSET_TTL).isAfter(Instant.now())) {
      return CompletableFuture.completedFuture(timed.value());
    }
    return call(() -> provider.asset(symbol)).thenApply(asset -> {
      assets.put(symbol, new Timed<>(asset, Instant.now()));
      return asset;
    });
  }

  /** The cached asset, if known, without fetching. */
  public Optional<Asset> cachedAsset(String symbol) {
    Timed<Optional<Asset>> timed = assets.get(symbol);
    return timed == null ? Optional.empty() : timed.value();
  }

  public CompletableFuture<List<DailyBar>> dailyBars(String symbol, int days) {
    String key = symbol + "|" + days;
    Timed<List<DailyBar>> timed = bars.get(key);
    if (timed != null && timed.at().plus(BARS_TTL).isAfter(Instant.now())) {
      return CompletableFuture.completedFuture(timed.value());
    }
    return call(() -> provider.dailyBars(symbol, days)).thenApply(result -> {
      bars.put(key, new Timed<>(List.copyOf(result), Instant.now()));
      return result;
    });
  }

  /** Market status, refetched each minute and whenever an open or close time has passed. */
  public CompletableFuture<MarketClock> clock() {
    Timed<MarketClock> current = clock;
    Instant now = Instant.now();
    if (current != null && current.at().plus(CLOCK_TTL).isAfter(now)
        && !passed(current.value().nextOpen(), now) && !passed(current.value().nextClose(), now)) {
      return CompletableFuture.completedFuture(current.value());
    }
    return call(provider::clock).thenApply(result -> {
      clock = new Timed<>(result, Instant.now());
      return result;
    });
  }

  private static boolean passed(Instant time, Instant now) {
    return time != null && !time.isAfter(now);
  }

  /** Runs a blocking provider call on the network executor. */
  public <T> CompletableFuture<T> call(ProviderCall<T> call) {
    return CompletableFuture.supplyAsync(wrap(call), network);
  }

  @FunctionalInterface
  public interface ProviderCall<T> {
    T get() throws MarketDataException;
  }

  private static <T> Supplier<T> wrap(ProviderCall<T> call) {
    return () -> {
      try {
        return call.get();
      } catch (MarketDataException e) {
        throw new CompletionException(e);
      }
    };
  }

  private record Timed<T>(T value, Instant at) {}
}
