package com.maldahleh.stockmarket;

import java.time.Clock;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * Where work runs and what it reports to, shared by the services.
 *
 * @param serverThread the Minecraft server thread; economy and Bukkit calls go here
 * @param databaseThread one thread, so database writes happen in order
 * @param network blocking calls to market data providers
 */
public record PluginContext(Executor serverThread, Executor databaseThread, Executor network,
    Clock clock, Logger logger) {}
