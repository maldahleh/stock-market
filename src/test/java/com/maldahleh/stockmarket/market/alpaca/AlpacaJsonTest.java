package com.maldahleh.stockmarket.market.alpaca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maldahleh.stockmarket.actions.CorporateAction;
import com.maldahleh.stockmarket.actions.CorporateAction.Merger;
import com.maldahleh.stockmarket.actions.CorporateAction.Split;
import com.maldahleh.stockmarket.market.AssetClass;
import com.maldahleh.stockmarket.market.Quote;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AlpacaJsonTest {

  private static JsonObject json(String text) {
    return JsonParser.parseString(text).getAsJsonObject();
  }

  @Test
  void parsesStockSnapshots() {
    Map<String, Quote> quotes = AlpacaJson.parseSnapshots(json("""
        {"AAPL": {
          "latestTrade": {"t": "2026-11-13T15:04:05.123456789Z", "p": 231.5, "s": 100},
          "dailyBar": {"o": 229, "h": 232.1, "l": 228.4, "c": 231.5, "v": 1234567},
          "prevDailyBar": {"c": 230}
        },
         "BAD": {"latestTrade": {"t": "2026-11-13T15:04:05Z", "p": 0}}}
        """), AssetClass.STOCK);

    assertEquals(1, quotes.size());
    Quote apple = quotes.get("AAPL");
    assertEquals(0, new BigDecimal("231.5").compareTo(apple.price()));
    assertEquals(Instant.parse("2026-11-13T15:04:05.123456789Z"), apple.tradeTime());
    assertEquals(0, new BigDecimal("230").compareTo(apple.previousClose()));
    assertEquals(1234567, apple.volume());
    assertEquals(new BigDecimal("0.65"), apple.changeFromClose().setScale(2, java.math.RoundingMode.HALF_UP));
  }

  @Test
  void parsesAssetStatus() {
    assertTrue(AlpacaJson.parseAsset(json("""
        {"symbol": "AAPL", "name": "Apple Inc.", "exchange": "NASDAQ", "status": "active",
         "tradable": true}""")).tradable());
    assertFalse(AlpacaJson.parseAsset(json("""
        {"symbol": "OLD", "name": "Old", "exchange": "NYSE", "status": "inactive",
         "tradable": true}""")).tradable());
  }

  @Test
  void parsesCorporateActionsAndDropsIncompleteOnes() {
    List<CorporateAction> actions = AlpacaJson.parseCorporateActions(json("""
        {"corporate_actions": {
          "forward_splits": [{"id": "f1", "symbol": "NVDA", "cusip": "x", "old_rate": 1,
            "new_rate": 10, "process_date": "2024-06-07", "ex_date": "2024-06-10"}],
          "reverse_splits": [{"id": "r1", "symbol": "ABC", "new_symbol": "ABCD", "old_rate": 20,
            "new_rate": 1, "process_date": "2026-01-02", "ex_date": "2026-01-05"},
            {"id": "broken", "symbol": "Z", "old_rate": 0, "new_rate": 1,
            "ex_date": "2026-01-05"}],
          "stock_and_cash_mergers": [{"id": "m1", "acquirer_symbol": "BIG",
            "acquirer_rate": 0.5, "acquiree_symbol": "SMALL", "acquiree_rate": 1,
            "cash_rate": 3.25, "process_date": "2026-02-01", "effective_date": "2026-02-02"}],
          "stock_dividends": [{"id": "d1", "symbol": "DIV", "rate": 0.05,
            "process_date": "2026-03-01", "ex_date": "2026-03-02"}]
        }, "next_page_token": null}"""));

    assertEquals(4, actions.size());
    Split forward = (Split) actions.get(0);
    assertEquals(LocalDate.of(2024, 6, 10), forward.effectiveDate());
    assertEquals("NVDA", forward.resultSymbol());
    assertEquals("ABCD", ((Split) actions.get(1)).resultSymbol());
    Split dividend = (Split) actions.get(2);
    assertEquals(0, new BigDecimal("1.05").compareTo(dividend.newRate()));
    Merger merger = (Merger) actions.get(3);
    assertTrue(merger.hasStock());
    assertEquals(0, new BigDecimal("3.25").compareTo(merger.cashPerShare()));
  }

  @Test
  void barsAreKeyedBySymbol() {
    assertEquals(2, AlpacaJson.parseBars(json("""
        {"bars": {"AAPL": [{"t": "2026-11-12T05:00:00Z", "o": 1, "h": 2, "l": 1, "c": 2,
          "v": 10}, {"t": "2026-11-11T05:00:00Z", "o": 1, "h": 2, "l": 1, "c": 2, "v": 10}]},
         "next_page_token": null}"""), "AAPL").size());
  }

  @Test
  void parsesUsdCashDividendsOnly() {
    List<CorporateAction> actions = AlpacaJson.parseCorporateActions(json("""
        {"corporate_actions": {"cash_dividends": [
          {"id": "c1", "symbol": "AAPL", "rate": 0.26, "special": false, "foreign": false,
           "process_date": "2026-11-14", "ex_date": "2026-11-10", "payable_date": "2026-11-14",
           "currency": "USD"},
          {"id": "c2", "symbol": "SHEL", "rate": 0.3, "special": false, "foreign": true,
           "process_date": "2026-11-14", "ex_date": "2026-11-10", "currency": "GBP"}
        ]}, "next_page_token": null}"""));
    assertEquals(1, actions.size());
    var dividend = (CorporateAction.CashDividend) actions.get(0);
    assertEquals(LocalDate.of(2026, 11, 10), dividend.effectiveDate());
    assertEquals(LocalDate.of(2026, 11, 14), dividend.dueDate());
  }
}
