package com.maldahleh.stockmarket.portfolio;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory copy of every holding, for menus and placeholders. The database is the source of
 * truth: this is only ever overwritten with holdings the database just returned.
 */
public class Portfolios {

  private final Map<UUID, Map<String, Holding>> holdings = new ConcurrentHashMap<>();

  public void load(Map<UUID, Map<String, Holding>> loaded) {
    holdings.clear();
    loaded.values().forEach(byOwner -> byOwner.values().forEach(this::put));
  }

  public void put(Holding holding) {
    if (holding.shares() == 0) {
      Map<String, Holding> byOwner = holdings.get(holding.owner());
      if (byOwner != null) {
        byOwner.remove(holding.symbol());
      }
      return;
    }
    holdings.computeIfAbsent(holding.owner(), k -> new ConcurrentHashMap<>())
        .put(holding.symbol(), holding);
  }

  public void putAll(Collection<Holding> updated) {
    updated.forEach(this::put);
  }

  public Optional<Holding> get(UUID owner, String symbol) {
    return Optional.ofNullable(holdings.getOrDefault(owner, Map.of()).get(symbol));
  }

  public List<Holding> of(UUID owner) {
    return holdings.getOrDefault(owner, Map.of()).values().stream()
        .sorted(java.util.Comparator.comparing(Holding::symbol))
        .toList();
  }

  /** Every player who holds anything. */
  public java.util.Set<UUID> owners() {
    java.util.Set<UUID> owners = new java.util.HashSet<>();
    holdings.forEach((owner, byOwner) -> {
      if (!byOwner.isEmpty()) {
        owners.add(owner);
      }
    });
    return owners;
  }

  /** Every symbol anyone holds. */
  public java.util.Set<String> heldSymbols() {
    java.util.Set<String> symbols = new java.util.TreeSet<>();
    holdings.values().forEach(byOwner -> symbols.addAll(byOwner.keySet()));
    return symbols;
  }
}
