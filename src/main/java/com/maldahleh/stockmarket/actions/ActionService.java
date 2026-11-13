package com.maldahleh.stockmarket.actions;

import com.maldahleh.stockmarket.actions.ActionApplier.MissingPriceException;
import com.maldahleh.stockmarket.actions.ActionApplier.Outcome;
import com.maldahleh.stockmarket.actions.ActionApplier.Position;
import com.maldahleh.stockmarket.actions.CorporateAction.Merger;
import com.maldahleh.stockmarket.actions.CorporateAction.SpinOff;
import com.maldahleh.stockmarket.actions.CorporateAction.Split;
import com.maldahleh.stockmarket.market.AssetClass;
import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.market.MarketDataException;
import com.maldahleh.stockmarket.market.Quote;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.storage.Database;
import com.maldahleh.stockmarket.trade.TradeRules;
import com.maldahleh.stockmarket.trade.Wallet;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checks held symbols for corporate actions and applies each one once. Actions from before the
 * plugin was installed are ignored, since holdings didn't exist then.
 */
public class ActionService {

  public static final String INSTALL_DATE_KEY = "install-date";

  private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
  private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
  private static final int LOOKBACK_DAYS = 60;
  private static final int LOOKAHEAD_DAYS = 14;

  /** Tells online players what happened to their shares. Called on the server thread. */
  @FunctionalInterface
  public interface Notifier {
    void notify(UUID player, CorporateAction action, BigDecimal payout);
  }

  private final MarketData market;
  private final Database database;
  private final Portfolios portfolios;
  private final Wallet wallet;
  private final TradeRules rules;
  private final ActionApplier applier;
  private final Notifier notifier;
  private final Executor network;
  private final Executor serverThread;
  private final Executor databaseThread;
  private final Clock clock;
  private final Logger logger;

  private final boolean dividends;
  private final Consumer<CorporateAction> afterApply;
  private final Supplier<Set<String>> watchedSymbols;
  private final Map<String, Set<Instant>> changes = new ConcurrentHashMap<>();
  private final AtomicBoolean running = new AtomicBoolean();
  private volatile boolean warnedUnavailable;

  public ActionService(MarketData market, Database database, Portfolios portfolios, Wallet wallet,
      TradeRules rules, boolean dividends, Notifier notifier, Consumer<CorporateAction> afterApply,
      Supplier<Set<String>> watchedSymbols, Executor network, Executor serverThread, Executor databaseThread, Clock clock,
      Logger logger) {
    this.dividends = dividends;
    this.afterApply = afterApply;
    this.watchedSymbols = watchedSymbols;
    this.market = market;
    this.database = database;
    this.portfolios = portfolios;
    this.wallet = wallet;
    this.rules = rules;
    this.applier = new ActionApplier(rules.priceMultiplier());
    this.notifier = notifier;
    this.network = network;
    this.serverThread = serverThread;
    this.databaseThread = databaseThread;
    this.clock = clock;
    this.logger = logger;
  }

  /** The moment an action took effect: market open, New York time, on its effective date. */
  public static Instant effectiveInstant(CorporateAction action) {
    return action.effectiveDate().atTime(MARKET_OPEN).atZone(MARKET_ZONE).toInstant();
  }

  private static Instant dueInstant(CorporateAction action) {
    return action.dueDate().atTime(MARKET_OPEN).atZone(MARKET_ZONE).toInstant();
  }

  /**
   * Whether a split, merger, rename, spin-off or removal of {@code symbol} took effect after
   * {@code after} and no later than {@code until}, as far as the last check knows.
   */
  public boolean changedBetween(String symbol, Instant after, Instant until) {
    return changes.getOrDefault(symbol, Set.of()).stream()
        .anyMatch(time -> time.isAfter(after) && !time.isAfter(until));
  }

  /** Runs one check unless one is already running. Call from the server thread. */
  public void runOnce() {
    if (!running.compareAndSet(false, true)) {
      return;
    }
    Set<String> held = new HashSet<>();
    Set<String> candidates = new HashSet<>(portfolios.heldSymbols());
    candidates.addAll(watchedSymbols.get());
    for (String symbol : candidates) {
      if (AssetClass.of(symbol) == AssetClass.STOCK) {
        held.add(symbol);
      }
    }
    if (held.isEmpty()) {
      running.set(false);
      return;
    }

    CompletableFuture.supplyAsync(() -> due(held), network)
        .thenCompose(this::applyInOrder)
        .whenComplete((ignored, error) -> {
          running.set(false);
          if (error != null) {
            report(error);
          }
        });
  }

  private List<CorporateAction> due(Set<String> held) {
    try {
      LocalDate today = LocalDate.now(clock.withZone(MARKET_ZONE));
      LocalDate installed = database.meta(INSTALL_DATE_KEY).map(LocalDate::parse).orElse(today);
      LocalDate from = today.minusDays(LOOKBACK_DAYS);
      if (from.isBefore(installed)) {
        from = installed;
      }
      Instant now = clock.instant();
      List<CorporateAction> due = new ArrayList<>();
      // Look ahead too, so open orders can be stopped the moment a known split takes effect.
      for (CorporateAction action : market.provider().corporateActions(held, from,
          today.plusDays(LOOKAHEAD_DAYS))) {
        if (!(action instanceof CorporateAction.CashDividend)) {
          Instant effective = effectiveInstant(action);
          for (String symbol : action.sourceSymbols()) {
            changes.computeIfAbsent(symbol, k -> ConcurrentHashMap.newKeySet()).add(effective);
          }
        }
        if (!dividends && action instanceof CorporateAction.CashDividend) {
          continue;
        }
        if (!action.effectiveDate().isBefore(installed)
            && !dueInstant(action).isAfter(now)
            && !database.isApplied(action.id())) {
          due.add(action);
        }
      }
      due.sort(Comparator.comparing(CorporateAction::dueDate));
      warnedUnavailable = false;
      return due;
    } catch (MarketDataException | SQLException e) {
      throw new CompletionException(e);
    }
  }

  private CompletableFuture<Void> applyInOrder(List<CorporateAction> actions) {
    CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
    for (CorporateAction action : actions) {
      chain = chain.thenCompose(ignored -> apply(action).exceptionally(error -> {
        Throwable cause = error instanceof CompletionException ? error.getCause() : error;
        if (cause instanceof MissingPriceException) {
          logger.info("Will retry " + describe(action) + " later: " + cause.getMessage());
        } else {
          logger.log(Level.SEVERE, "Couldn't apply " + describe(action), cause);
        }
        return null;
      }));
    }
    return chain;
  }

  private CompletableFuture<Void> apply(CorporateAction action) {
    return pricesFor(action)
        .thenApplyAsync(prices -> compute(action, prices), databaseThread)
        .thenAcceptAsync(outcome -> {
          if (outcome == null) {
            return;
          }
          portfolios.putAll(outcome.holdings());
          afterApply.accept(action);
          Set<UUID> affected = new LinkedHashSet<>();
          outcome.entries().forEach(entry -> affected.add(entry.owner()));
          for (UUID player : affected) {
            BigDecimal payout = outcome.payouts().getOrDefault(player, BigDecimal.ZERO);
            if (payout.signum() > 0 && !wallet.deposit(player, payout)) {
              logger.severe("Couldn't pay " + payout + " to " + player + " for "
                  + describe(action) + "; pay it by hand.");
            }
            notifier.notify(player, action, payout);
          }
          logger.info("Applied " + describe(action) + " to " + affected.size() + " player(s).");
        }, serverThread);
  }

  /** Runs on the database thread so it sees every trade saved before it. */
  private Outcome compute(CorporateAction action, Map<String, BigDecimal> prices) {
    try {
      Instant effective = effectiveInstant(action);
      Map<UUID, Map<String, Holding>> existing = new HashMap<>();
      List<Position> positions = new ArrayList<>();
      for (String symbol : action.sourceSymbols()) {
        Set<UUID> owners = new LinkedHashSet<>();
        database.holdersOf(symbol).forEach(h -> owners.add(h.owner()));
        owners.addAll(database.recentSellers(symbol, effective));
        for (UUID owner : owners) {
          Map<String, Holding> theirs = existing.computeIfAbsent(owner, o -> holdings(o));
          Holding holding = theirs.getOrDefault(symbol,
              new Holding(owner, symbol, 0, BigDecimal.ZERO));
          long changed = database.sharesChangedSince(owner, symbol, effective);
          positions.add(new Position(holding, Math.max(0, holding.shares() - changed)));
        }
      }
      Outcome outcome = applier.apply(action, positions, existing, prices::get, clock.instant());
      return database.applyAction(action.id(), outcome) ? outcome : null;
    } catch (SQLException | MissingPriceException e) {
      throw new CompletionException(e);
    }
  }

  private Map<String, Holding> holdings(UUID owner) {
    try {
      return database.holdingsOf(owner);
    } catch (SQLException e) {
      throw new CompletionException(e);
    }
  }

  /** Server prices for every symbol the action may pay out in. */
  private CompletableFuture<Map<String, BigDecimal>> pricesFor(CorporateAction action) {
    Set<String> symbols = new HashSet<>();
    switch (action) {
      case Split s -> symbols.add(s.resultSymbol());
      case Merger m -> {
        if (m.hasStock()) {
          symbols.add(m.acquirer());
        }
      }
      case SpinOff s -> {
        symbols.add(s.source());
        symbols.add(s.newSymbol());
      }
      default -> { }
    }
    if (symbols.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of());
    }
    return market.quotes(symbols).thenApply(quotes -> {
      Map<String, BigDecimal> prices = new HashMap<>();
      for (Quote quote : quotes.values()) {
        prices.put(quote.symbol(), rules.serverPrice(quote.price()));
      }
      return prices;
    });
  }

  private void report(Throwable error) {
    Throwable cause = error instanceof CompletionException ? error.getCause() : error;
    if (cause instanceof MarketDataException e && (e.status() == 403 || e.status() == 401)) {
      if (!warnedUnavailable) {
        warnedUnavailable = true;
        logger.warning("Corporate actions are unavailable: " + e.getMessage()
            + " Splits and mergers won't be applied to holdings until this works.");
      }
      return;
    }
    logger.log(Level.WARNING, "Corporate action check failed", cause);
  }

  public static String describe(CorporateAction action) {
    return switch (action) {
      case Split s -> s.symbol() + " split " + s.newRate().stripTrailingZeros().toPlainString()
          + "-for-" + s.oldRate().stripTrailingZeros().toPlainString();
      case CorporateAction.NameChange n -> n.oldSymbol() + " renamed to " + n.newSymbol();
      case Merger m -> m.acquiree() + (m.hasStock() ? " merged into " + m.acquirer()
          : " bought out for cash");
      case SpinOff s -> s.source() + " spun off " + s.newSymbol();
      case CorporateAction.Removal r -> r.symbol() + " removed as worthless";
      case CorporateAction.CashDividend d -> d.symbol() + " paid a dividend of $"
          + d.rate().stripTrailingZeros().toPlainString() + " a share";
    };
  }
}
