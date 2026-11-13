package com.maldahleh.stockmarket.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.maldahleh.stockmarket.FakeProvider;
import com.maldahleh.stockmarket.FakeWallet;
import com.maldahleh.stockmarket.PluginContext;
import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.storage.Database;
import com.maldahleh.stockmarket.trade.TradeResult.Failure;
import com.maldahleh.stockmarket.trade.TradeResult.Reason;
import com.maldahleh.stockmarket.trade.TradeResult.Success;
import java.io.File;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TradeServiceTest {

  private static final UUID P = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-11-13T16:00:00Z");
  private static final Executor DIRECT = Runnable::run;

  @TempDir
  File dir;

  private final FakeProvider provider = new FakeProvider();
  private final FakeWallet wallet = new FakeWallet();
  private final Portfolios portfolios = new Portfolios();
  private Database database;
  private TradeService trades;

  private static TradeRules rules() {
    return new TradeRules(new BigDecimal("10"), new BigDecimal("1.00"), new BigDecimal("1"),
        true, true, new BigDecimal("1"), Duration.ofMinutes(15), true, Set.of("OTC"), 1000);
  }

  @BeforeEach
  void setUp() throws Exception {
    database = Database.sqlite(new File(dir, "test.db"));
    trades = service(DIRECT, rules());
    provider.price("AAPL", "100", NOW.minusSeconds(30));
    wallet.deposit(P, new BigDecimal("5000"));
  }

  private TradeService service(Executor databaseThread, TradeRules rules) {
    return new TradeService(new MarketData(provider, DIRECT, Duration.ofMinutes(5)), database,
        portfolios, wallet, rules, new PluginContext(DIRECT, databaseThread, DIRECT,
            Clock.fixed(NOW, ZoneOffset.UTC), Logger.getAnonymousLogger()));
  }

  @AfterEach
  void tearDown() {
    database.close();
  }

  private TradeResult buy(long shares) {
    return trades.buy(P, "aapl", shares).join();
  }

  private TradeResult sell(long shares) {
    return trades.sell(P, "AAPL", shares).join();
  }

  @Test
  void buyChargesPriceTimesMultiplierPlusFees() throws Exception {
    Success success = assertInstanceOf(Success.class, buy(2));
    // 2 × $100 × 10 = 2000; fee 1% + 1.00 = 21.00
    assertEquals(0, new BigDecimal("2021.00").compareTo(success.total()));
    assertEquals(0, new BigDecimal("2979.00").compareTo(wallet.balance(P)));
    assertEquals(2, portfolios.get(P, "AAPL").orElseThrow().shares());
    assertEquals(2, database.holdingsOf(P).get("AAPL").shares());
  }

  @Test
  void sellPaysProceedsAndReportsNet() {
    buy(2);
    provider.price("AAPL", "110", NOW.minusSeconds(5));
    Success sale = assertInstanceOf(Success.class, sell(2));
    // 2 × 110 × 10 = 2200, fee 23.00 → 2177.00; cost was 2021.00
    assertEquals(0, new BigDecimal("2177.00").compareTo(sale.total()));
    assertEquals(0, new BigDecimal("156.00").compareTo(sale.net()));
    assertEquals(0, new BigDecimal("5156.00").compareTo(wallet.balance(P)));
    assertEquals(0, portfolios.of(P).size());
  }

  @Test
  void cannotSellMoreThanOwned() {
    buy(1);
    assertEquals(Reason.INSUFFICIENT_SHARES, ((Failure) sell(2)).reason());
  }

  @Test
  void twoSalesOfTheSameSharesCanNotBothSucceed() {
    buy(1);
    // Hold database work so both sales are in progress at once, as with a double click.
    List<Runnable> queued = new ArrayList<>();
    trades = service(queued::add, rules());
    var first = trades.sell(P, "AAPL", 1);
    var second = trades.sell(P, "AAPL", 1);
    queued.forEach(Runnable::run);
    assertInstanceOf(Success.class, first.join());
    assertEquals(Reason.BUSY, ((Failure) second.join()).reason());
    assertEquals(0, new BigDecimal("4978.00").compareTo(wallet.balance(P)));
  }

  @Test
  void stalePriceIsRefused() {
    provider.price("AAPL", "100", NOW.minus(Duration.ofMinutes(20)));
    assertEquals(Reason.STALE_PRICE, ((Failure) buy(1)).reason());
    assertEquals(0, new BigDecimal("5000").compareTo(wallet.balance(P)));
  }

  @Test
  void closedMarketIsRefusedForStocksButNotCrypto() {
    provider.open = false;
    assertEquals(Reason.MARKET_CLOSED, ((Failure) buy(1)).reason());
    provider.price("BTC/USD", "90", NOW.minusSeconds(1));
    assertInstanceOf(Success.class, trades.buy(P, "btc/usd", 1).join());
  }

  @Test
  void notEnoughMoneyMovesNothing() {
    assertEquals(Reason.INSUFFICIENT_FUNDS, ((Failure) buy(10)).reason());
    assertEquals(0, portfolios.of(P).size());
  }

  @Test
  void blockedExchangeAndUnknownSymbolAreRefused() {
    provider.price("PINK", "5", NOW);
    provider.assets.put("PINK", new com.maldahleh.stockmarket.market.Asset("PINK", "Pink",
        "OTC", true));
    assertEquals(Reason.NOT_TRADABLE, ((Failure) trades.buy(P, "PINK", 1).join()).reason());
    assertEquals(Reason.INVALID_SYMBOL, ((Failure) trades.buy(P, "NOPE", 1).join()).reason());
  }

  @Test
  void failedSaveRefundsThePurchase() {
    database.close();
    assertEquals(Reason.STORAGE_ERROR, ((Failure) buy(1)).reason());
    assertEquals(0, new BigDecimal("5000").compareTo(wallet.balance(P)));
  }

  @Test
  void sellingBelowTheMinimumPriceIsAllowed() {
    buy(1);
    provider.price("AAPL", "0.50", NOW);
    assertInstanceOf(Success.class, sell(1));
    assertEquals(Reason.PRICE_TOO_LOW, ((Failure) buy(1)).reason());
  }

  @Test
  void limitBuyRefusesAPriceAboveTheLimit() {
    assertEquals(Reason.PRICE_MOVED,
        ((Failure) trades.buyAtMost(P, "AAPL", 1, new BigDecimal("999")).join()).reason());
    assertInstanceOf(Success.class,
        trades.buyAtMost(P, "AAPL", 1, new BigDecimal("1000")).join());
  }


  @Test
  void cryptoTradesInFractionsWithNoMinimumPrice() {
    provider.price("BTC/USD", "60000", NOW.minusSeconds(1));
    provider.price("DOGE/USD", "0.20", NOW.minusSeconds(1));
    // 0.005 BTC × $60,000 × 10 = 3000 is too much; 0.001 BTC = 600 plus fee 7.00
    long units = com.maldahleh.stockmarket.market.Quantities.parse("BTC/USD", "0.001");
    Success btc = assertInstanceOf(Success.class, trades.buy(P, "BTC/USD", units).join());
    assertEquals(0, new BigDecimal("607.00").compareTo(btc.total()));
    assertEquals(100_000, portfolios.get(P, "BTC/USD").orElseThrow().shares());
    // DOGE is below the $1 minimum but crypto is exempt
    long doge = com.maldahleh.stockmarket.market.Quantities.parse("DOGE/USD", "10");
    assertInstanceOf(Success.class, trades.buy(P, "DOGE/USD", doge).join());
  }

  @Test
  void closedMarketSaysWhenItReopens() {
    provider.open = false;
    Failure closed = (Failure) buy(1);
    assertEquals(Reason.MARKET_CLOSED, closed.reason());
    org.junit.jupiter.api.Assertions.assertNotNull(closed.when());
  }
}
