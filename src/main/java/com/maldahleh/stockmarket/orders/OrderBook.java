package com.maldahleh.stockmarket.orders;

import com.maldahleh.stockmarket.PluginContext;
import com.maldahleh.stockmarket.market.Asset;
import com.maldahleh.stockmarket.market.AssetClass;
import com.maldahleh.stockmarket.market.MarketClock;
import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.market.Quote;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.storage.Database;
import com.maldahleh.stockmarket.trade.TradeResult;
import com.maldahleh.stockmarket.trade.TradeResult.Failure;
import com.maldahleh.stockmarket.trade.TradeResult.Reason;
import com.maldahleh.stockmarket.trade.TradeResult.Success;
import com.maldahleh.stockmarket.trade.TradeRules;
import com.maldahleh.stockmarket.trade.TradeService;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Limit orders and stop-losses. Each check compares every open order with the latest price;
 * a triggered order runs as a normal trade at a freshly fetched price, which for limit orders
 * must still be within the limit. No money is held while an order waits, so a buy is cancelled
 * if the player can't afford it when it fills.
 *
 * <p>Call public methods on the server thread.
 */
public class OrderBook {

  /** Failures that mean the order can never fill as placed. */
  private static final Set<Reason> FATAL = EnumSet.of(Reason.INSUFFICIENT_FUNDS,
      Reason.INSUFFICIENT_SHARES, Reason.NOT_TRADABLE, Reason.INVALID_SYMBOL,
      Reason.CRYPTO_DISABLED, Reason.TOO_MANY_SHARES, Reason.INVALID_QUANTITY);

  private final MarketData market;
  private final Database database;
  private final Portfolios portfolios;
  private final TradeService trades;
  private final TradeRules rules;
  private final int maxOpenPerPlayer;
  private final Duration lifetime;
  private final Notifier notifier;
  private final ActionWatch actionWatch;
  private final Executor serverThread;
  private final Executor databaseThread;
  private final Clock clock;
  private final Logger logger;

  private final Map<Long, Order> open = new ConcurrentHashMap<>();
  private final AtomicBoolean checking = new AtomicBoolean();

  public OrderBook(MarketData market, Database database, Portfolios portfolios,
      TradeService trades, TradeRules rules, Limits limits, Notifier notifier,
      ActionWatch actionWatch, PluginContext context) {
    this.market = market;
    this.database = database;
    this.portfolios = portfolios;
    this.trades = trades;
    this.rules = rules;
    this.maxOpenPerPlayer = limits.maxOpenPerPlayer();
    this.lifetime = limits.lifetime();
    this.notifier = notifier;
    this.actionWatch = actionWatch;
    this.serverThread = context.serverThread();
    this.databaseThread = context.databaseThread();
    this.clock = context.clock();
    this.logger = context.logger();
  }

  public void load(List<Order> orders) {
    open.clear();
    orders.forEach(order -> open.put(order.id(), order));
  }

  public List<Order> of(UUID owner) {
    return open.values().stream().filter(o -> o.owner().equals(owner))
        .sorted(Comparator.comparingLong(Order::id)).toList();
  }

  public CompletableFuture<Placement> place(UUID owner, OrderType type, String rawSymbol,
      long shares, BigDecimal target) {
    String symbol = rawSymbol.trim().toUpperCase();
    Problem problem = requestProblem(owner, type, symbol, shares, target);
    if (problem != null) {
      return rejected(problem);
    }
    return market.asset(symbol)
        .handleAsync((asset, error) -> error != null ? Problem.DATA_UNAVAILABLE
            : assetProblem(asset), serverThread)
        .thenCompose(assetProblem -> assetProblem != null ? rejected(assetProblem)
            : save(new Order(0, owner, symbol, type, shares,
                target.setScale(2, java.math.RoundingMode.HALF_UP), clock.instant(),
                clock.instant().plus(lifetime))));
  }

  /** Checks that need no market data. Null means the request is fine so far. */
  private Problem requestProblem(UUID owner, OrderType type, String symbol, long shares,
      BigDecimal target) {
    Problem input = inputProblem(symbol, shares, target);
    if (input != null) {
      return input;
    }
    if (of(owner).size() >= maxOpenPerPlayer) {
      return Problem.TOO_MANY_ORDERS;
    }
    if (AssetClass.of(symbol) == AssetClass.CRYPTO && !rules.allowCrypto()) {
      return Problem.CRYPTO_DISABLED;
    }
    return !type.buying() && owned(owner, symbol) < shares ? Problem.NOT_ENOUGH_SHARES : null;
  }

  private Problem inputProblem(String symbol, long shares, BigDecimal target) {
    if (shares <= 0) {
      return Problem.INVALID_QUANTITY;
    }
    if (rules.tooLarge(symbol, shares)) {
      return Problem.TOO_MANY_SHARES;
    }
    return target == null || target.signum() <= 0 ? Problem.INVALID_PRICE : null;
  }

  private Problem assetProblem(Optional<Asset> asset) {
    if (asset.isEmpty()) {
      return Problem.INVALID_SYMBOL;
    }
    Asset a = asset.get();
    boolean blocked = a.exchange() != null
        && rules.blockedExchanges().contains(a.exchange().toUpperCase());
    return !a.tradable() || blocked ? Problem.NOT_TRADABLE : null;
  }

  private CompletableFuture<Placement> save(Order order) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return database.insertOrder(order);
      } catch (SQLException e) {
        throw new CompletionException(e);
      }
    }, databaseThread).handleAsync((saved, error) -> {
      if (error != null) {
        logger.log(Level.SEVERE, "Couldn't save an order", error);
        return (Placement) new Placement.Rejected(Problem.STORAGE_ERROR);
      }
      open.put(saved.id(), saved);
      return new Placement.Placed(saved);
    }, serverThread);
  }

  /** @return false if the player has no open order with that id */
  public boolean cancel(UUID owner, long id) {
    Order order = open.get(id);
    if (order == null || !order.owner().equals(owner)) {
      return false;
    }
    close(order, CancelCause.BY_PLAYER, false);
    return true;
  }

  /** Cancels any player's order, for moderators. */
  public boolean cancelAny(long id) {
    Order order = open.get(id);
    if (order == null) {
      return false;
    }
    close(order, CancelCause.BY_MODERATOR, true);
    return true;
  }

  public int maxOpenPerPlayer() {
    return maxOpenPerPlayer;
  }

  /** Every open order, for the status report. */
  public int openCount() {
    return open.size();
  }

  /** Cancels every open order on the symbols, e.g. after a split changed what a price means. */
  public void cancelSymbols(Set<String> symbols, CancelCause cause) {
    for (Order order : List.copyOf(open.values())) {
      if (symbols.contains(order.symbol())) {
        close(order, cause, true);
      }
    }
  }

  /** Every symbol with an open order. */
  public Set<String> symbols() {
    Set<String> symbols = new TreeSet<>();
    open.values().forEach(order -> symbols.add(order.symbol()));
    return symbols;
  }

  /** Expires old orders and fills triggered ones. Runs one check at a time. */
  public void check() {
    if (open.isEmpty() || !checking.compareAndSet(false, true)) {
      return;
    }
    closeStale(clock.instant());
    Set<String> symbols = symbols();
    if (symbols.isEmpty()) {
      checking.set(false);
      return;
    }

    boolean anyStocks = symbols.stream().anyMatch(s -> AssetClass.of(s) == AssetClass.STOCK);
    CompletableFuture<MarketClock> marketClock = anyStocks ? market.clock()
        : CompletableFuture.completedFuture(null);
    marketClock.thenCombine(market.quotes(symbols), this::triggered)
        .thenComposeAsync(this::fillInOrder, serverThread)
        .whenComplete((ignored, error) -> {
          checking.set(false);
          if (error != null) {
            logger.log(Level.WARNING, "Order check failed", error);
          }
        });
  }

  /** Closes expired orders, and orders a split or merger has made meaningless. */
  private void closeStale(Instant now) {
    for (Order order : List.copyOf(open.values())) {
      if (!order.expires().isAfter(now)) {
        close(order, CancelCause.EXPIRED, true);
      } else if (actionWatch.changedBetween(order.symbol(), order.created(), now)) {
        close(order, CancelCause.CORPORATE_ACTION, true);
      }
    }
  }

  /** Open orders whose target the latest prices have reached, oldest first. */
  private List<Order> triggered(MarketClock status, Map<String, Quote> quotes) {
    boolean stocksOpen = status != null && status.open();
    List<Order> triggered = new ArrayList<>();
    for (Order order : open.values()) {
      boolean tradingNow = stocksOpen || AssetClass.of(order.symbol()) != AssetClass.STOCK;
      Quote quote = quotes.get(order.symbol());
      if (tradingNow && quote != null
          && order.type().triggeredBy(rules.serverPrice(quote.price()), order.target())) {
        triggered.add(order);
      }
    }
    triggered.sort(Comparator.comparingLong(Order::id));
    return triggered;
  }

  private CompletableFuture<Void> fillInOrder(List<Order> triggered) {
    CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
    for (Order order : triggered) {
      chain = chain.thenCompose(ignored -> open.containsKey(order.id()) ? fill(order)
          : CompletableFuture.completedFuture(null));
    }
    return chain;
  }

  private CompletableFuture<Void> fill(Order order) {
    CompletableFuture<TradeResult> trade = switch (order.type()) {
      case LIMIT_BUY -> trades.buyAtMost(order.owner(), order.symbol(), order.shares(),
          order.target());
      case LIMIT_SELL -> trades.sellAtLeast(order.owner(), order.symbol(), order.shares(),
          order.target());
      case STOP_SELL -> trades.sellAtLeast(order.owner(), order.symbol(), order.shares(), null);
    };
    return trade.thenAcceptAsync(result -> {
      if (result instanceof Success success) {
        remove(order);
        notifier.filled(order, success);
      } else if (result instanceof Failure failure && FATAL.contains(failure.reason())) {
        close(order, switch (failure.reason()) {
          case INSUFFICIENT_FUNDS -> CancelCause.NOT_ENOUGH_MONEY;
          case INSUFFICIENT_SHARES -> CancelCause.NOT_ENOUGH_SHARES;
          default -> CancelCause.NOT_TRADABLE;
        }, true);
      }
      // Anything else (price moved, market closed, stale price) waits for the next check.
    }, serverThread);
  }

  private void close(Order order, CancelCause cause, boolean notify) {
    remove(order);
    if (notify) {
      notifier.cancelled(order, cause);
    }
  }

  private void remove(Order order) {
    if (open.remove(order.id()) == null) {
      return;
    }
    databaseThread.execute(() -> {
      try {
        database.deleteOrder(order.id());
      } catch (SQLException e) {
        logger.log(Level.SEVERE, "Couldn't delete order " + order.id(), e);
      }
    });
  }

  private long owned(UUID owner, String symbol) {
    return portfolios.get(owner, symbol).map(Holding::shares).orElse(0L);
  }

  private static CompletableFuture<Placement> rejected(Problem problem) {
    return CompletableFuture.completedFuture(new Placement.Rejected(problem));
  }

  /** How many orders a player may keep open, and for how long. */
  public record Limits(int maxOpenPerPlayer, Duration lifetime) {}

  /** Why an order closed without filling. */
  public enum CancelCause {
    BY_PLAYER,
    BY_MODERATOR,
    EXPIRED,
    NOT_ENOUGH_MONEY,
    NOT_ENOUGH_SHARES,
    NOT_TRADABLE,
    CORPORATE_ACTION
  }

  public sealed interface Placement {
    record Placed(Order order) implements Placement {}

    record Rejected(Problem problem) implements Placement {}
  }

  public enum Problem {
    INVALID_QUANTITY,
    TOO_MANY_SHARES,
    INVALID_PRICE,
    TOO_MANY_ORDERS,
    NOT_ENOUGH_SHARES,
    INVALID_SYMBOL,
    NOT_TRADABLE,
    CRYPTO_DISABLED,
    DATA_UNAVAILABLE,
    STORAGE_ERROR
  }

  /** Whether a corporate action changed the symbol's price scale between two instants. */
  @FunctionalInterface
  public interface ActionWatch {
    boolean changedBetween(String symbol, Instant after, Instant until);
  }

  public interface Notifier {
    void filled(Order order, Success result);

    void cancelled(Order order, CancelCause cause);
  }
}
