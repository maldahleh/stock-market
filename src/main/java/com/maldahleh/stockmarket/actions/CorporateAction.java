package com.maldahleh.stockmarket.actions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * An event that changes what a share is worth or what it is called. Each one applies to holdings
 * that existed before it took effect.
 */
public sealed interface CorporateAction {

  /** The provider's id; used to apply each action exactly once. */
  String id();

  /**
   * The first trading day the market priced the change in. Shares held before market open that
   * day are the ones the action applies to.
   */
  LocalDate effectiveDate();

  /** The day the action is applied; later than {@link #effectiveDate()} only for dividends. */
  default LocalDate dueDate() {
    return effectiveDate();
  }

  /** Symbols whose holders are affected. */
  Set<String> sourceSymbols();

  /**
   * Shares are multiplied by {@code newRate / oldRate}. Covers forward and reverse splits and
   * stock dividends. A reverse split may also change the symbol.
   */
  record Split(
      String id, LocalDate effectiveDate, String symbol, String newSymbol, BigDecimal oldRate,
      BigDecimal newRate) implements CorporateAction {

    @Override
    public Set<String> sourceSymbols() {
      return Set.of(symbol);
    }

    public String resultSymbol() {
      return newSymbol == null || newSymbol.isBlank() ? symbol : newSymbol;
    }
  }

  record NameChange(String id, LocalDate effectiveDate, String oldSymbol, String newSymbol)
      implements CorporateAction {

    @Override
    public Set<String> sourceSymbols() {
      return Set.of(oldSymbol);
    }
  }

  /**
   * The acquiree's shares are replaced by cash and, for stock deals, acquirer shares. Each acquiree
   * share becomes {@code acquirerRate / acquireeRate} acquirer shares plus {@code cashPerShare}.
   * Also used for redemptions (cash only, no acquirer).
   */
  record Merger(
      String id, LocalDate effectiveDate, String acquiree, String acquirer,
      BigDecimal acquireeRate, BigDecimal acquirerRate, BigDecimal cashPerShare)
      implements CorporateAction {

    @Override
    public Set<String> sourceSymbols() {
      return Set.of(acquiree);
    }

    public boolean hasStock() {
      return acquirer != null && !acquirer.isBlank() && acquirerRate != null
          && acquirerRate.signum() > 0;
    }
  }

  /** Holders keep their shares and also receive {@code newRate / sourceRate} new shares each. */
  record SpinOff(
      String id, LocalDate effectiveDate, String source, String newSymbol, BigDecimal sourceRate,
      BigDecimal newRate) implements CorporateAction {

    @Override
    public Set<String> sourceSymbols() {
      return Set.of(source);
    }
  }

  /**
   * A cash payment per share to everyone who held shares before the ex-dividend date, paid on
   * the payable date.
   */
  record CashDividend(String id, LocalDate effectiveDate, LocalDate payableDate, String symbol,
      BigDecimal rate) implements CorporateAction {

    @Override
    public LocalDate dueDate() {
      return payableDate == null || payableDate.isBefore(effectiveDate) ? effectiveDate
          : payableDate;
    }

    @Override
    public Set<String> sourceSymbols() {
      return Set.of(symbol);
    }
  }

  /** The shares were cancelled and are now worth nothing. */
  record Removal(String id, LocalDate effectiveDate, String symbol) implements CorporateAction {

    @Override
    public Set<String> sourceSymbols() {
      return Set.of(symbol);
    }
  }
}
