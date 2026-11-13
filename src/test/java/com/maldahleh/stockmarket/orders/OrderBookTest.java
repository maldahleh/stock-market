package com.maldahleh.stockmarket.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maldahleh.stockmarket.FakeProvider;
import com.maldahleh.stockmarket.FakeWallet;
import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.orders.OrderBook.CancelCause;
import com.maldahleh.stockmarket.orders.OrderBook.Placement;
import com.maldahleh.stockmarket.orders.OrderBook.Problem;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.storage.Database;
import com.maldahleh.stockmarket.trade.TradeResult.Success;
import com.maldahleh.stockmarket.trade.TradeRules;
import com.maldahleh.stockmarket.trade.TradeService;
import java.io.File;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OrderBookTest {

  private static final UUID P = UUID.randomUUID();
  private static final Executor DIRECT = Runnable::run;

  @TempDir
  File dir;

  private final FakeProvider provider = new FakeProvider();
  private final FakeWallet wallet = new FakeWallet();
  private final Portfolios portfolios = new Portfolios();
  private final List<Order> filled = new ArrayList<>();
  private final List<CancelCause> cancelled = new ArrayList<>();
  private Instant now = Instant.parse("2026-11-13T16:00:00Z");
  private boolean splitHappened;
  private Database database;
  private TradeService trades;
  private OrderBook book;

  @BeforeEach
  void setUp() throws Exception {
    database = Database.sqlite(new File(dir, "test.db"));
    // Price multiplier 1 and no fees keep the numbers readable.
    TradeRules rules = new TradeRules(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, true,
        true, BigDecimal.ONE, Duration.ofMinutes(15), true, Set.of(), 1000, Duration.ZERO);
    Clock clock = new Clock() {
      @Override
      public ZoneOffset getZone() {
        return ZoneOffset.UTC;
      }

      @Override
      public Clock withZone(java.time.ZoneId zone) {
        return this;
      }

      @Override
      public Instant instant() {
        return now;
      }
    };
    MarketData market = new MarketData(provider, DIRECT, Duration.ofNanos(1));
    trades = new TradeService(market, database, portfolios, wallet, rules, DIRECT, DIRECT, clock,
        Logger.getAnonymousLogger());
    book = new OrderBook(market, database, portfolios, trades, rules, 3, Duration.ofDays(30),
        new OrderBook.Notifier() {
          @Override
          public void filled(Order order, Success result) {
            filled.add(order);
          }

          @Override
          public void cancelled(Order order, CancelCause cause) {
            cancelled.add(cause);
          }
        }, (symbol, after, until) -> splitHappened, DIRECT, DIRECT, clock,
        Logger.getAnonymousLogger());
    provider.price("AAPL", "100", now);
    wallet.deposit(P, new BigDecimal("1000"));
  }

  @AfterEach
  void tearDown() {
    database.close();
  }

  private Order place(OrderType type, long shares, String target) {
    Placement placement = book.place(P, type, "aapl", shares, new BigDecimal(target)).join();
    return assertInstanceOf(Placement.Placed.class, placement).order();
  }

  private void priceAt(String price) {
    now = now.plusSeconds(60);
    provider.price("AAPL", price, now);
    book.check();
  }

  @Test
  void limitBuyWaitsThenFillsWhenThePriceDrops() throws Exception {
    place(OrderType.LIMIT_BUY, 2, "90");
    priceAt("95");
    assertTrue(filled.isEmpty());
    priceAt("89.50");
    assertEquals(1, filled.size());
    assertEquals(2, portfolios.get(P, "AAPL").orElseThrow().shares());
    assertEquals(0, new BigDecimal("821.00").compareTo(wallet.balance(P)));
    assertTrue(book.of(P).isEmpty());
    assertTrue(database.loadOrders().isEmpty());
  }

  @Test
  void limitSellAndStopLossFill() {
    trades.buy(P, "AAPL", 2).join();
    place(OrderType.LIMIT_SELL, 1, "120");
    place(OrderType.STOP_SELL, 1, "80");
    priceAt("121");
    assertEquals(OrderType.LIMIT_SELL, filled.get(0).type());
    priceAt("79");
    assertEquals(OrderType.STOP_SELL, filled.get(1).type());
    assertTrue(portfolios.of(P).isEmpty());
  }

  @Test
  void sellOrdersNeedTheShares() {
    Placement placement = book.place(P, OrderType.STOP_SELL, "AAPL", 1, BigDecimal.TEN).join();
    assertEquals(Problem.NOT_ENOUGH_SHARES,
        assertInstanceOf(Placement.Rejected.class, placement).problem());
  }

  @Test
  void buyThatCantBeAffordedIsCancelled() {
    place(OrderType.LIMIT_BUY, 50, "90");
    priceAt("85");
    assertEquals(List.of(CancelCause.NOT_ENOUGH_MONEY), cancelled);
    assertTrue(book.of(P).isEmpty());
  }

  @Test
  void ordersWaitWhileTheMarketIsClosed() {
    place(OrderType.LIMIT_BUY, 1, "90");
    provider.open = false;
    priceAt("80");
    assertTrue(filled.isEmpty());
    assertEquals(1, book.of(P).size());
    provider.open = true;
    priceAt("80");
    assertEquals(1, filled.size());
  }

  @Test
  void splitCancelsOpenOrders() {
    place(OrderType.LIMIT_BUY, 1, "90");
    splitHappened = true;
    priceAt("10");
    assertTrue(filled.isEmpty());
    assertEquals(List.of(CancelCause.CORPORATE_ACTION), cancelled);
  }

  @Test
  void ordersExpire() {
    place(OrderType.LIMIT_BUY, 1, "50");
    now = now.plus(Duration.ofDays(31));
    priceAt("95");
    assertEquals(List.of(CancelCause.EXPIRED), cancelled);
  }

  @Test
  void openOrdersAreLimitedAndSurviveARestart() throws Exception {
    place(OrderType.LIMIT_BUY, 1, "50");
    place(OrderType.LIMIT_BUY, 1, "60");
    place(OrderType.LIMIT_BUY, 1, "70");
    assertEquals(Problem.TOO_MANY_ORDERS, assertInstanceOf(Placement.Rejected.class,
        book.place(P, OrderType.LIMIT_BUY, "AAPL", 1, BigDecimal.TEN).join()).problem());
    assertEquals(3, database.loadOrders().size());
    assertEquals(0, new BigDecimal("50.00").compareTo(database.loadOrders().get(0).target()));
    assertTrue(book.cancel(P, book.of(P).get(0).id()));
    assertEquals(2, database.loadOrders().size());
    assertEquals(Map.of(), Map.of());
  }
}
