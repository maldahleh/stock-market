package com.maldahleh.stockmarket.actions;

import com.maldahleh.stockmarket.actions.CorporateAction.CashDividend;
import com.maldahleh.stockmarket.actions.CorporateAction.Merger;
import com.maldahleh.stockmarket.actions.CorporateAction.NameChange;
import com.maldahleh.stockmarket.actions.CorporateAction.Removal;
import com.maldahleh.stockmarket.actions.CorporateAction.SpinOff;
import com.maldahleh.stockmarket.actions.CorporateAction.Split;
import com.maldahleh.stockmarket.portfolio.EntryType;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.LedgerEntry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Works out what a corporate action does to each holder. Pure: it reads positions and prices and
 * returns the changes; the caller saves them and pays out the cash.
 *
 * <p>Whole shares are kept; any fraction is paid out in cash at the current server price, the way
 * a broker does. Payouts round down so the plugin never creates money.
 */
public final class ActionApplier {

  private static final int MATH_SCALE = 10;

  private final BigDecimal priceMultiplier;

  public ActionApplier(BigDecimal priceMultiplier) {
    this.priceMultiplier = priceMultiplier;
  }

  /**
   * @param existing a holder's current holding of any symbol the action pays shares into, so they
   *     can be merged; looked up by owner and symbol
   */
  public Outcome apply(CorporateAction action, List<Position> positions,
      Map<UUID, Map<String, Holding>> existing, Prices prices, Instant now)
      throws MissingPriceException {
    Builder out = new Builder(existing, now, action.id());
    for (Position position : positions) {
      if (position.holding().shares() == 0 && position.sharesAtEffective() == 0) {
        continue;
      }
      switch (action) {
        case Split s -> split(s, position, prices, out);
        case NameChange n -> rename(n, position, out);
        case Merger m -> merger(m, position, prices, out);
        case SpinOff s -> spinOff(s, position, prices, out);
        case Removal r -> removal(position, out);
        case CashDividend d -> dividend(d, position, out);
      }
    }
    return out.build();
  }

  private void split(Split split, Position position, Prices prices, Builder out)
      throws MissingPriceException {
    Holding current = position.holding();
    if (position.sharesAtEffective() <= 0) {
      return;
    }
    BigDecimal ratio = split.newRate().divide(split.oldRate(), MATH_SCALE, RoundingMode.HALF_UP);
    BigDecimal held = BigDecimal.valueOf(position.sharesAtEffective());
    BigDecimal after = BigDecimal.valueOf(current.shares()).add(held.multiply(ratio).subtract(held))
        .max(BigDecimal.ZERO);
    String symbol = split.resultSymbol();

    long whole = after.setScale(0, RoundingMode.DOWN).longValueExact();
    BigDecimal fraction = after.subtract(BigDecimal.valueOf(whole));
    BigDecimal basis = after.signum() == 0 ? BigDecimal.ZERO
        : current.costBasis().multiply(BigDecimal.valueOf(whole))
            .divide(after, 2, RoundingMode.HALF_UP);

    if (symbol.equals(current.symbol())) {
      out.addTo(current.owner(), symbol, whole - current.shares(),
          basis.subtract(current.costBasis()));
      out.entry(current.owner(), EntryType.SPLIT, symbol, whole - current.shares(), null,
          BigDecimal.ZERO);
    } else {
      out.remove(current);
      out.addTo(current.owner(), symbol, whole, basis);
      out.entry(current.owner(), EntryType.SPLIT, current.symbol(), -current.shares(), null,
          BigDecimal.ZERO);
      out.entry(current.owner(), EntryType.SPLIT, symbol, whole, null, BigDecimal.ZERO);
    }
    cashInLieu(current.owner(), symbol, fraction, prices, out);
  }

  private void rename(NameChange change, Position position, Builder out) {
    Holding current = position.holding();
    if (current.shares() == 0) {
      return;
    }
    out.remove(current);
    out.addTo(current.owner(), change.newSymbol(), current.shares(), current.costBasis());
    out.entry(current.owner(), EntryType.RENAME, change.oldSymbol(), -current.shares(), null,
        BigDecimal.ZERO);
    out.entry(current.owner(), EntryType.RENAME, change.newSymbol(), current.shares(), null,
        BigDecimal.ZERO);
  }

  private void merger(Merger merger, Position position, Prices prices, Builder out)
      throws MissingPriceException {
    Holding current = position.holding();
    BigDecimal held = BigDecimal.valueOf(current.shares());
    if (current.shares() == 0) {
      return;
    }
    BigDecimal cash = money(held.multiply(nonNull(merger.cashPerShare())).multiply(priceMultiplier));

    out.remove(current);
    out.entry(current.owner(), EntryType.MERGER, current.symbol(), -current.shares(), null, cash);
    out.pay(current.owner(), cash);

    if (!merger.hasStock()) {
      return;
    }
    BigDecimal exact = held.multiply(merger.acquirerRate())
        .divide(merger.acquireeRate(), MATH_SCALE, RoundingMode.HALF_UP);
    long whole = exact.setScale(0, RoundingMode.DOWN).longValueExact();
    BigDecimal price = require(prices, merger.acquirer());

    // Split the old cost between the new shares and the cash by what each is worth.
    BigDecimal stockValue = price.multiply(exact);
    BigDecimal total = stockValue.add(cash);
    BigDecimal stockBasis = total.signum() == 0 ? current.costBasis()
        : current.costBasis().multiply(stockValue).divide(total, MATH_SCALE, RoundingMode.HALF_UP);
    BigDecimal wholeBasis = exact.signum() == 0 ? BigDecimal.ZERO
        : stockBasis.multiply(BigDecimal.valueOf(whole)).divide(exact, 2, RoundingMode.HALF_UP);

    out.addTo(current.owner(), merger.acquirer(), whole, wholeBasis);
    out.entry(current.owner(), EntryType.MERGER, merger.acquirer(), whole, null, BigDecimal.ZERO);
    cashInLieu(current.owner(), merger.acquirer(), exact.subtract(BigDecimal.valueOf(whole)),
        prices, out);
  }

  private void spinOff(SpinOff spinOff, Position position, Prices prices, Builder out)
      throws MissingPriceException {
    Holding current = position.holding();
    if (position.sharesAtEffective() <= 0) {
      return;
    }
    BigDecimal exact = BigDecimal.valueOf(position.sharesAtEffective())
        .multiply(spinOff.newRate())
        .divide(spinOff.sourceRate(), MATH_SCALE, RoundingMode.HALF_UP);
    long whole = exact.setScale(0, RoundingMode.DOWN).longValueExact();
    BigDecimal newPrice = require(prices, spinOff.newSymbol());

    // Move part of the parent's cost to the new shares, by market value.
    BigDecimal moved = BigDecimal.ZERO;
    if (current.shares() > 0 && current.costBasis().signum() > 0) {
      BigDecimal sourceValue = require(prices, spinOff.source())
          .multiply(BigDecimal.valueOf(current.shares()));
      BigDecimal newValue = newPrice.multiply(exact);
      moved = current.costBasis().multiply(newValue)
          .divide(sourceValue.add(newValue), 2, RoundingMode.HALF_UP);
      out.addTo(current.owner(), current.symbol(), 0, moved.negate());
    }

    BigDecimal wholeMoved = exact.signum() == 0 ? BigDecimal.ZERO
        : moved.multiply(BigDecimal.valueOf(whole)).divide(exact, 2, RoundingMode.HALF_UP);
    out.addTo(current.owner(), spinOff.newSymbol(), whole, wholeMoved);
    out.entry(current.owner(), EntryType.SPIN_OFF, spinOff.newSymbol(), whole, null,
        BigDecimal.ZERO);
    cashInLieu(current.owner(), spinOff.newSymbol(), exact.subtract(BigDecimal.valueOf(whole)),
        prices, out);
  }

  private void removal(Position position, Builder out) {
    Holding current = position.holding();
    if (current.shares() == 0) {
      return;
    }
    out.remove(current);
    out.entry(current.owner(), EntryType.REMOVED, current.symbol(), -current.shares(), null,
        BigDecimal.ZERO);
  }

  private void dividend(CashDividend dividend, Position position, Builder out) {
    long eligible = position.sharesAtEffective();
    if (eligible <= 0) {
      return;
    }
    BigDecimal cash = money(BigDecimal.valueOf(eligible).multiply(dividend.rate())
        .multiply(priceMultiplier));
    if (cash.signum() > 0) {
      out.entry(position.holding().owner(), EntryType.DIVIDEND, dividend.symbol(), 0, null, cash);
      out.pay(position.holding().owner(), cash);
    }
  }

  private void cashInLieu(UUID owner, String symbol, BigDecimal fraction, Prices prices,
      Builder out) throws MissingPriceException {
    if (fraction.signum() <= 0) {
      return;
    }
    BigDecimal cash = money(fraction.multiply(require(prices, symbol)));
    if (cash.signum() > 0) {
      out.entry(owner, EntryType.CASH_IN_LIEU, symbol, 0, null, cash);
      out.pay(owner, cash);
    }
  }

  private static BigDecimal require(Prices prices, String symbol) throws MissingPriceException {
    BigDecimal price = prices.serverPrice(symbol);
    if (price == null || price.signum() <= 0) {
      throw new MissingPriceException(symbol);
    }
    return price;
  }

  private static BigDecimal nonNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static BigDecimal money(BigDecimal value) {
    return value.setScale(2, RoundingMode.DOWN);
  }

  /** Collects changes, merging repeated changes to the same holding. */
  private static final class Builder {

    private final Map<UUID, Map<String, Holding>> existing;
    private final Map<String, Holding> holdings = new LinkedHashMap<>();
    private final List<LedgerEntry> entries = new ArrayList<>();
    private final Map<UUID, BigDecimal> payouts = new HashMap<>();
    private final Instant now;
    private final String reference;

    Builder(Map<UUID, Map<String, Holding>> existing, Instant now, String reference) {
      this.existing = existing;
      this.now = now;
      this.reference = reference;
    }

    void remove(Holding holding) {
      holdings.put(key(holding.owner(), holding.symbol()),
          new Holding(holding.owner(), holding.symbol(), 0, BigDecimal.ZERO));
    }

    void addTo(UUID owner, String symbol, long shares, BigDecimal basis) {
      Holding base = holdings.get(key(owner, symbol));
      if (base == null) {
        base = existing.getOrDefault(owner, Map.of()).get(symbol);
      }
      if (base == null) {
        base = new Holding(owner, symbol, 0, BigDecimal.ZERO);
      }
      long newShares = base.shares() + shares;
      BigDecimal newBasis = newShares == 0 ? BigDecimal.ZERO
          : base.costBasis().add(basis).max(BigDecimal.ZERO);
      holdings.put(key(owner, symbol), new Holding(owner, symbol, newShares, newBasis));
    }

    void entry(UUID owner, EntryType type, String symbol, long shares, BigDecimal price,
        BigDecimal cash) {
      entries.add(new LedgerEntry(owner, type, symbol, shares, price, BigDecimal.ZERO, cash, now,
          reference));
    }

    void pay(UUID owner, BigDecimal amount) {
      if (amount.signum() > 0) {
        payouts.merge(owner, amount, BigDecimal::add);
      }
    }

    Outcome build() {
      return new Outcome(List.copyOf(holdings.values()), List.copyOf(entries), Map.copyOf(payouts));
    }

    private static String key(UUID owner, String symbol) {
      return owner + "|" + symbol;
    }
  }

  /** Server price per share, or null if it can't be found right now. */
  @FunctionalInterface
  public interface Prices {
    BigDecimal serverPrice(String symbol);
  }

  /** Thrown when a price the math needs is missing; the action should be retried later. */
  public static class MissingPriceException extends Exception {
    public MissingPriceException(String symbol) {
      super("No price for " + symbol);
    }
  }

  /**
   * A holder of the action's source symbol.
   *
   * @param sharesAtEffective shares they held just before the action took effect. Shares bought
   *     later were bought at the new price and must not be adjusted again.
   */
  public record Position(Holding holding, long sharesAtEffective) {}

  /**
   * @param holdings new state of every holding touched; zero shares means delete it
   * @param payouts money to give each player, already in server currency
   */
  public record Outcome(List<Holding> holdings, List<LedgerEntry> entries,
      Map<UUID, BigDecimal> payouts) {}
}
