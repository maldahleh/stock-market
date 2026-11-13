package com.maldahleh.stockmarket.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maldahleh.stockmarket.actions.ActionApplier.MissingPriceException;
import com.maldahleh.stockmarket.actions.ActionApplier.Outcome;
import com.maldahleh.stockmarket.actions.ActionApplier.Position;
import com.maldahleh.stockmarket.actions.CorporateAction.CashDividend;
import com.maldahleh.stockmarket.actions.CorporateAction.Merger;
import com.maldahleh.stockmarket.actions.CorporateAction.NameChange;
import com.maldahleh.stockmarket.actions.CorporateAction.Removal;
import com.maldahleh.stockmarket.actions.CorporateAction.SpinOff;
import com.maldahleh.stockmarket.actions.CorporateAction.Split;
import com.maldahleh.stockmarket.portfolio.Holding;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionApplierTest {

  private static final UUID P = UUID.randomUUID();
  private static final LocalDate DAY = LocalDate.of(2026, 11, 2);
  private final ActionApplier applier = new ActionApplier(BigDecimal.ONE);

  private static BigDecimal d(String value) {
    return new BigDecimal(value);
  }

  private static Holding h(String symbol, long shares, String basis) {
    return new Holding(P, symbol, shares, d(basis));
  }

  private Outcome apply(CorporateAction action, Position position, Map<String, String> prices,
      Holding... others) throws MissingPriceException {
    Map<String, Holding> existing = new HashMap<>();
    existing.put(position.holding().symbol(), position.holding());
    for (Holding other : others) {
      existing.put(other.symbol(), other);
    }
    return applier.apply(action, List.of(position), Map.of(P, existing),
        symbol -> prices.containsKey(symbol) ? d(prices.get(symbol)) : null, Instant.EPOCH);
  }

  private static Holding find(Outcome outcome, String symbol) {
    return outcome.holdings().stream().filter(h -> h.symbol().equals(symbol)).findFirst()
        .orElseThrow();
  }

  @Test
  void forwardSplitMultipliesSharesAndKeepsCost() throws Exception {
    Split split = new Split("a", DAY, "NVDA", null, d("1"), d("10"));
    Outcome out = apply(split, new Position(h("NVDA", 3, "3000"), 3), Map.of("NVDA", "100"));
    assertEquals(30, find(out, "NVDA").shares());
    assertEquals(0, d("3000").compareTo(find(out, "NVDA").costBasis()));
    assertTrue(out.payouts().isEmpty());
  }

  @Test
  void sharesBoughtAfterTheSplitAreNotMultipliedAgain() throws Exception {
    // Held 3 before a 2-for-1 split, then bought 4 at the new price.
    Split split = new Split("a", DAY, "X", null, d("1"), d("2"));
    Outcome out = apply(split, new Position(h("X", 7, "700"), 3), Map.of("X", "50"));
    assertEquals(10, find(out, "X").shares());
  }

  @Test
  void sharesSoldAfterTheSplitAreStillOwed() throws Exception {
    // Held 10 before a 2-for-1 split, sold all 10 at the new price before it was applied.
    Split split = new Split("a", DAY, "X", null, d("1"), d("2"));
    Outcome out = apply(split, new Position(h("X", 0, "0"), 10), Map.of("X", "50"));
    assertEquals(10, find(out, "X").shares());
  }

  @Test
  void reverseSplitPaysTheFractionInCash() throws Exception {
    // 25 shares, 1-for-10: 2 whole shares and half a share paid at $40.
    Split split = new Split("a", DAY, "Y", "YY", d("10"), d("1"));
    Outcome out = apply(split, new Position(h("Y", 25, "250"), 25), Map.of("YY", "40"));
    assertEquals(0, find(out, "Y").shares());
    assertEquals(2, find(out, "YY").shares());
    assertEquals(0, d("200.00").compareTo(find(out, "YY").costBasis()));
    assertEquals(0, d("20.00").compareTo(out.payouts().get(P)));
  }

  @Test
  void fractionWithoutAPriceIsRetriedLater() {
    Split split = new Split("a", DAY, "Y", null, d("10"), d("1"));
    assertThrows(MissingPriceException.class,
        () -> apply(split, new Position(h("Y", 25, "250"), 25), Map.of()));
  }

  @Test
  void renameMovesHoldingAndMergesWithExisting() throws Exception {
    NameChange change = new NameChange("a", DAY, "FB", "META");
    Outcome out = apply(change, new Position(h("FB", 5, "500"), 5), Map.of(),
        h("META", 2, "1000"));
    assertEquals(0, find(out, "FB").shares());
    assertEquals(7, find(out, "META").shares());
    assertEquals(0, d("1500").compareTo(find(out, "META").costBasis()));
  }

  @Test
  void cashMergerPaysOutAndRemoves() throws Exception {
    Merger merger = new Merger("a", DAY, "ATVI", null, null, null, d("95"));
    Outcome out = apply(merger, new Position(h("ATVI", 4, "300"), 4), Map.of());
    assertEquals(0, find(out, "ATVI").shares());
    assertEquals(0, d("380.00").compareTo(out.payouts().get(P)));
  }

  @Test
  void cashUsesThePriceMultiplier() throws Exception {
    ActionApplier doubled = new ActionApplier(d("20"));
    Merger merger = new Merger("a", DAY, "ATVI", null, null, null, d("95"));
    Outcome out = doubled.apply(merger, List.of(new Position(h("ATVI", 1, "1900"), 1)),
        Map.of(), s -> null, Instant.EPOCH);
    assertEquals(0, d("1900.00").compareTo(out.payouts().get(P)));
  }

  @Test
  void stockMergerConvertsSharesWithCashInLieu() throws Exception {
    // Each acquiree share becomes 0.5 acquirer shares; 3 shares → 1 whole + 0.5 in cash.
    Merger merger = new Merger("a", DAY, "SMALL", "BIG", d("1"), d("0.5"), BigDecimal.ZERO);
    Outcome out = apply(merger, new Position(h("SMALL", 3, "150"), 3), Map.of("BIG", "100"));
    assertEquals(0, find(out, "SMALL").shares());
    assertEquals(1, find(out, "BIG").shares());
    assertEquals(0, d("50.00").compareTo(out.payouts().get(P)));
  }

  @Test
  void spinOffAddsNewSharesAndMovesSomeCost() throws Exception {
    SpinOff spinOff = new SpinOff("a", DAY, "PARENT", "CHILD", d("1"), d("1"));
    Outcome out = apply(spinOff, new Position(h("PARENT", 10, "1000"), 10),
        Map.of("PARENT", "75", "CHILD", "25"));
    assertEquals(10, find(out, "CHILD").shares());
    assertEquals(0, d("250.00").compareTo(find(out, "CHILD").costBasis()));
    assertEquals(0, d("750.00").compareTo(find(out, "PARENT").costBasis()));
    assertEquals(10, find(out, "PARENT").shares());
  }

  @Test
  void removalZeroesTheHolding() throws Exception {
    Outcome out = apply(new Removal("a", DAY, "DEAD"), new Position(h("DEAD", 9, "90"), 9),
        Map.of());
    assertEquals(0, find(out, "DEAD").shares());
    assertTrue(out.payouts().isEmpty());
  }

  @Test
  void dividendPaysSharesHeldBeforeTheExDate() throws Exception {
    // Held 10 before the ex-date, bought 5 more after: only the 10 are paid, at $0.25 × 20.
    ActionApplier doubled = new ActionApplier(d("20"));
    CashDividend dividend = new CashDividend("a", DAY, DAY.plusDays(14), "AAPL", d("0.25"));
    Outcome out = doubled.apply(dividend, List.of(new Position(h("AAPL", 15, "100"), 10)),
        Map.of(), s -> null, Instant.EPOCH);
    assertEquals(0, d("50.00").compareTo(out.payouts().get(P)));
    assertTrue(out.holdings().isEmpty());
  }

  @Test
  void dividendIsStillPaidAfterSellingTheShares() throws Exception {
    CashDividend dividend = new CashDividend("a", DAY, DAY.plusDays(14), "AAPL", d("1"));
    Outcome out = apply(dividend, new Position(h("AAPL", 0, "0"), 3), Map.of());
    assertEquals(0, d("3.00").compareTo(out.payouts().get(P)));
  }

  @Test
  void dividendIsDueOnThePayableDate() {
    CashDividend dividend = new CashDividend("a", DAY, DAY.plusDays(14), "AAPL", d("1"));
    assertEquals(DAY.plusDays(14), dividend.dueDate());
    assertEquals(DAY, dividend.effectiveDate());
  }
}
