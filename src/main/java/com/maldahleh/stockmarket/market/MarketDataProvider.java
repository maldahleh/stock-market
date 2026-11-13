package com.maldahleh.stockmarket.market;

import com.maldahleh.stockmarket.actions.CorporateAction;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A source of prices. Every method blocks on the network; never call one on the server thread. */
public interface MarketDataProvider {

  /** Latest prices for the given symbols. Unknown symbols are left out of the result. */
  Map<String, Quote> quotes(Collection<String> symbols) throws MarketDataException;

  Optional<Asset> asset(String symbol) throws MarketDataException;

  /** Up to {@code days} most recent daily bars, oldest first, adjusted for splits. */
  List<DailyBar> dailyBars(String symbol, int days) throws MarketDataException;

  MarketClock clock() throws MarketDataException;

  /** Splits, mergers, renames and removals processed between the two dates, inclusive. */
  List<CorporateAction> corporateActions(Collection<String> symbols, LocalDate from, LocalDate to)
      throws MarketDataException;
}
