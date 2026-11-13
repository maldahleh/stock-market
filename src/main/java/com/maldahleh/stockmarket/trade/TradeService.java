package com.maldahleh.stockmarket.trade;

import com.maldahleh.stockmarket.market.Asset;
import com.maldahleh.stockmarket.market.AssetClass;
import com.maldahleh.stockmarket.market.MarketClock;
import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.market.Quote;
import com.maldahleh.stockmarket.portfolio.EntryType;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.LedgerEntry;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.storage.Database;
import com.maldahleh.stockmarket.storage.Database.InsufficientSharesException;
import com.maldahleh.stockmarket.storage.Database.Sale;
import com.maldahleh.stockmarket.trade.TradeResult.Failure;
import com.maldahleh.stockmarket.trade.TradeResult.Reason;
import com.maldahleh.stockmarket.trade.TradeResult.Success;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Buys and sells at a price fetched for the trade itself.
 *
 * <p>Money and shares move in an order that can't create either: a purchase takes the money
 * first and refunds it if the shares can't be saved; a sale removes the shares in the database
 * first, which also stops two sales of the same shares, and pays only after that succeeds. One
 * trade per player runs at a time.
 *
 * <p>Call {@link #buy} and {@link #sell} on the server thread. Their futures complete there too.
 */
public class TradeService {

  private final MarketData market;
  private final Database database;
  private final Portfolios portfolios;
  private final Wallet wallet;
  private final TradeRules rules;
  private final Executor serverThread;
  private final Executor databaseThread;
  private final Clock clock;
  private final Logger logger;

  private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();
  private final Map<UUID, Instant> lastTrade = new ConcurrentHashMap<>();

  public TradeService(MarketData market, Database database, Portfolios portfolios, Wallet wallet,
      TradeRules rules, Executor serverThread, Executor databaseThread, Clock clock,
      Logger logger) {
    this.market = market;
    this.database = database;
    this.portfolios = portfolios;
    this.wallet = wallet;
    this.rules = rules;
    this.serverThread = serverThread;
    this.databaseThread = databaseThread;
    this.clock = clock;
    this.logger = logger;
  }

  public TradeRules rules() {
    return rules;
  }

  private record Priced(Asset asset, Quote quote, BigDecimal price) {}

  public CompletableFuture<TradeResult> buy(UUID player, String symbol, long shares) {
    return buy(player, symbol, shares, null, false);
  }

  /** A buy for a limit order: skips the cooldown and refuses a server price above the limit. */
  public CompletableFuture<TradeResult> buyAtMost(UUID player, String symbol, long shares,
      BigDecimal limit) {
    return buy(player, symbol, shares, limit, true);
  }

  public CompletableFuture<TradeResult> sell(UUID player, String symbol, long shares) {
    return sell(player, symbol, shares, null, false);
  }

  /**
   * A sale for an order: skips the cooldown and refuses a server price below {@code limit}. A
   * null limit sells at the market price (stop-loss).
   */
  public CompletableFuture<TradeResult> sellAtLeast(UUID player, String symbol, long shares,
      BigDecimal limit) {
    return sell(player, symbol, shares, limit, true);
  }

  private CompletableFuture<TradeResult> buy(UUID player, String rawSymbol, long shares,
      BigDecimal limit, boolean fromOrder) {
    String symbol = normalize(rawSymbol);
    return run(player, shares, fromOrder, () -> price(symbol, true).thenComposeAsync(priced -> {
      if (priced instanceof Failure failure) {
        return done(failure);
      }
      Priced p = (Priced) priced;
      if (limit != null && p.price().compareTo(limit) > 0) {
        return done(new Failure(Reason.PRICE_MOVED));
      }
      BigDecimal value = p.price().multiply(BigDecimal.valueOf(shares));
      BigDecimal fee = rules.fee(value, true);
      BigDecimal total = value.add(fee);
      if (!wallet.withdraw(player, total)) {
        return done(new Failure(Reason.INSUFFICIENT_FUNDS));
      }

      LedgerEntry entry = new LedgerEntry(player, EntryType.BUY, symbol, shares, p.price(), fee,
          total.negate(), clock.instant(), null);
      return CompletableFuture.supplyAsync(() -> {
        try {
          return database.buy(player, symbol, shares, total, entry);
        } catch (SQLException e) {
          throw new CompletionException(e);
        }
      }, databaseThread).handleAsync((holding, error) -> {
        if (error != null) {
          logger.log(Level.SEVERE, "Couldn't save a purchase; refunding " + total, error);
          if (!wallet.deposit(player, total)) {
            logger.severe("Refund of " + total + " to " + player + " failed; pay it by hand.");
          }
          return new Failure(Reason.STORAGE_ERROR);
        }
        portfolios.put(holding);
        return new Success(entry, p.asset(), holding, value, fee, total, null);
      }, serverThread);
    }, serverThread));
  }

  private CompletableFuture<TradeResult> sell(UUID player, String rawSymbol, long shares,
      BigDecimal limit, boolean fromOrder) {
    String symbol = normalize(rawSymbol);
    long owned = portfolios.get(player, symbol).map(Holding::shares).orElse(0L);
    if (owned < shares && shares > 0) {
      return done(new Failure(Reason.INSUFFICIENT_SHARES));
    }

    return run(player, shares, fromOrder, () -> price(symbol, false).thenComposeAsync(priced -> {
      if (priced instanceof Failure failure) {
        return done(failure);
      }
      Priced p = (Priced) priced;
      if (limit != null && p.price().compareTo(limit) < 0) {
        return done(new Failure(Reason.PRICE_MOVED));
      }
      BigDecimal value = p.price().multiply(BigDecimal.valueOf(shares));
      BigDecimal fee = rules.fee(value, false);
      BigDecimal proceeds = value.subtract(fee);
      if (proceeds.signum() <= 0) {
        return done(new Failure(Reason.PRICE_TOO_LOW));
      }

      LedgerEntry entry = new LedgerEntry(player, EntryType.SELL, symbol, -shares, p.price(), fee,
          proceeds, clock.instant(), null);
      return CompletableFuture.supplyAsync(() -> {
        try {
          return database.sell(player, symbol, shares, entry);
        } catch (SQLException | InsufficientSharesException e) {
          throw new CompletionException(e);
        }
      }, databaseThread).handleAsync((Sale sale, Throwable error) -> {
        if (error != null) {
          Throwable cause = error instanceof CompletionException ? error.getCause() : error;
          if (cause instanceof InsufficientSharesException) {
            return new Failure(Reason.INSUFFICIENT_SHARES);
          }
          logger.log(Level.SEVERE, "Couldn't save a sale; nothing was sold", cause);
          return new Failure(Reason.STORAGE_ERROR);
        }
        portfolios.put(sale.holding());
        if (!wallet.deposit(player, proceeds)) {
          logger.severe("Sold " + shares + " " + symbol + " for " + player
              + " but the economy refused the payment of " + proceeds + "; pay it by hand.");
        }
        return new Success(entry, p.asset(), sale.holding(), value, fee, proceeds,
            proceeds.subtract(sale.basisRemoved()));
      }, serverThread);
    }, serverThread));
  }

  /** Shared checks, the in-flight lock and the cooldown. */
  private CompletableFuture<TradeResult> run(UUID player, long shares, boolean fromOrder,
      java.util.function.Supplier<CompletableFuture<TradeResult>> trade) {
    if (shares <= 0) {
      return done(new Failure(Reason.INVALID_QUANTITY));
    }
    if (shares > rules.maxSharesPerTrade()) {
      return done(new Failure(Reason.TOO_MANY_SHARES));
    }
    Instant last = lastTrade.get(player);
    if (!fromOrder && last != null && last.plus(rules.cooldown()).isAfter(clock.instant())) {
      return done(new Failure(Reason.COOLDOWN));
    }
    if (!inFlight.add(player)) {
      return done(new Failure(Reason.BUSY));
    }

    CompletableFuture<TradeResult> result;
    try {
      result = trade.get();
    } catch (RuntimeException e) {
      inFlight.remove(player);
      throw e;
    }
    return result.handleAsync((outcome, error) -> {
      inFlight.remove(player);
      if (error != null) {
        logger.log(Level.SEVERE, "Trade failed unexpectedly", error);
        return new Failure(Reason.DATA_UNAVAILABLE);
      }
      if (outcome instanceof Success && !fromOrder) {
        lastTrade.put(player, clock.instant());
      }
      return outcome;
    }, serverThread);
  }

  /**
   * Fetches what a trade needs and checks it is safe to trade at. Completes with either a
   * {@link Priced} or a {@link Failure}.
   */
  private CompletableFuture<Object> price(String symbol, boolean buying) {
    AssetClass assetClass = AssetClass.of(symbol);
    if (assetClass == AssetClass.CRYPTO && !rules.allowCrypto()) {
      return CompletableFuture.completedFuture(new Failure(Reason.CRYPTO_DISABLED));
    }

    CompletableFuture<Optional<Asset>> asset = market.asset(symbol);
    CompletableFuture<MarketClock> marketClock = assetClass == AssetClass.STOCK
        ? market.clock() : CompletableFuture.completedFuture(null);
    CompletableFuture<Optional<Quote>> quote = market.freshQuote(symbol);

    return CompletableFuture.allOf(asset, marketClock, quote).handle((ignored, error) -> {
      if (error != null) {
        logger.warning("Couldn't get market data for " + symbol + ": " + rootMessage(error));
        return new Failure(Reason.DATA_UNAVAILABLE);
      }
      Optional<Asset> foundAsset = asset.join();
      if (foundAsset.isEmpty()) {
        return new Failure(Reason.INVALID_SYMBOL);
      }
      Asset a = foundAsset.get();
      if (!a.tradable() || (a.exchange() != null
          && rules.blockedExchanges().contains(a.exchange().toUpperCase()))) {
        return new Failure(Reason.NOT_TRADABLE);
      }
      MarketClock c = marketClock.join();
      if (c != null && !c.open()) {
        return new Failure(Reason.MARKET_CLOSED);
      }
      Optional<Quote> q = quote.join();
      if (q.isEmpty()) {
        return new Failure(Reason.DATA_UNAVAILABLE);
      }
      Quote current = q.get();
      if (Duration.between(current.tradeTime(), clock.instant()).compareTo(rules.maxQuoteAge())
          > 0) {
        return new Failure(Reason.STALE_PRICE);
      }
      // Selling is always allowed, so holders of a falling stock can still get out.
      if (buying && current.price().compareTo(rules.minimumPrice()) < 0) {
        return new Failure(Reason.PRICE_TOO_LOW);
      }
      return new Priced(a, current, rules.serverPrice(current.price()));
    });
  }

  private static String rootMessage(Throwable error) {
    Throwable cause = error;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause.getMessage();
  }

  static String normalize(String symbol) {
    return symbol.trim().toUpperCase();
  }

  private static CompletableFuture<TradeResult> done(TradeResult result) {
    return CompletableFuture.completedFuture(result);
  }
}
