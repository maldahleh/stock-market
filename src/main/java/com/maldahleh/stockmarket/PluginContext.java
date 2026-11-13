package com.maldahleh.stockmarket;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Where work runs and what it reports to, shared by the services.
 *
 * @param serverThread the Minecraft server thread; economy and Bukkit calls go here
 * @param databaseThread one thread, so database writes happen in order
 * @param network blocking calls to market data providers
 * @param names a player's last known name, or null if unknown
 */
public record PluginContext(Executor serverThread, Executor databaseThread, Executor network,
    Clock clock, Logger logger, Function<UUID, String> names) {

  /** "Steve (8f3c...)" for console messages an admin has to act on. */
  public String describe(UUID player) {
    String name = names.apply(player);
    return name == null ? player.toString() : name + " (" + player + ")";
  }
}
