package com.maldahleh.stockmarket;

import com.maldahleh.stockmarket.actions.CorporateAction;
import com.maldahleh.stockmarket.market.Asset;
import com.maldahleh.stockmarket.market.AssetClass;
import com.maldahleh.stockmarket.market.DailyBar;
import com.maldahleh.stockmarket.market.MarketClock;
import com.maldahleh.stockmarket.market.MarketDataException;
import com.maldahleh.stockmarket.market.MarketDataProvider;
import com.maldahleh.stockmarket.market.Quote;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeProvider implements MarketDataProvider {

  public final Map<String, Quote> quotes = new HashMap<>();
  public final Map<String, Asset> assets = new HashMap<>();
  public final List<CorporateAction> actions = new ArrayList<>();
  public boolean open = true;
  public int quoteCalls;

  public void price(String symbol, String price, Instant time) {
    quotes.put(symbol, new Quote(symbol, AssetClass.of(symbol), new BigDecimal(price), time,
        null, null, null, null, 0));
    assets.putIfAbsent(symbol, new Asset(symbol, symbol + " Inc.", "NASDAQ", true));
  }

  @Override
  public Map<String, Quote> quotes(Collection<String> symbols) {
    quoteCalls++;
    Map<String, Quote> result = new HashMap<>();
    symbols.forEach(s -> {
      if (quotes.containsKey(s)) {
        result.put(s, quotes.get(s));
      }
    });
    return result;
  }

  @Override
  public Optional<Asset> asset(String symbol) {
    return Optional.ofNullable(assets.get(symbol));
  }

  @Override
  public List<DailyBar> dailyBars(String symbol, int days) {
    return List.of();
  }

  @Override
  public MarketClock clock() {
    // An open or close time that has already passed makes MarketData ask again next time,
    // so tests can flip the market between checks.
    Instant passed = Instant.now();
    return new MarketClock(open, open ? null : passed, open ? passed : null, passed);
  }

  @Override
  public List<CorporateAction> corporateActions(Collection<String> symbols, LocalDate from,
      LocalDate to) throws MarketDataException {
    return actions.stream().filter(a -> a.sourceSymbols().stream().anyMatch(symbols::contains))
        .toList();
  }
}
