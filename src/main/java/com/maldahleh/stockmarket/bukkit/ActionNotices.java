package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.actions.CorporateAction;
import com.maldahleh.stockmarket.actions.CorporateAction.CashDividend;
import com.maldahleh.stockmarket.actions.CorporateAction.Merger;
import com.maldahleh.stockmarket.actions.CorporateAction.NameChange;
import com.maldahleh.stockmarket.actions.CorporateAction.Removal;
import com.maldahleh.stockmarket.actions.CorporateAction.SpinOff;
import com.maldahleh.stockmarket.actions.CorporateAction.Split;
import com.maldahleh.stockmarket.trade.TradeRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Plain-language messages for corporate actions, one {@code messages.yml} key per kind. Money is
 * shown in server money, the same as everywhere else in game.
 */
public final class ActionNotices {

  /** A message key and its placeholders. */
  public record Notice(String key, Map<String, String> values) {}

  private ActionNotices() {}

  public static Notice of(CorporateAction action, BigDecimal payout, TradeRules rules,
      Formats formats, Messages messages) {
    Map<String, String> values = new HashMap<>();
    values.put("<payout>", formats.money(payout));
    values.put("<paid>", payout.signum() > 0
        ? messages.raw("corporate-actions.paid").replace("<payout>", formats.money(payout)) : "");
    String key = switch (action) {
      case Split s -> split(s, values);
      case NameChange n -> {
        values.put("<symbol>", n.oldSymbol());
        values.put("<new-symbol>", n.newSymbol());
        yield "rename";
      }
      case Merger m -> merger(m, values, rules, formats);
      case SpinOff s -> {
        values.put("<symbol>", s.source());
        values.put("<new-symbol>", s.newSymbol());
        values.put("<ratio>", plain(s.newRate().divide(s.sourceRate(), 6, RoundingMode.HALF_UP)));
        yield "spin-off";
      }
      case Removal r -> {
        values.put("<symbol>", r.symbol());
        yield "removed";
      }
      case CashDividend d -> {
        values.put("<symbol>", d.symbol());
        values.put("<per-share>", formats.money(d.rate().multiply(rules.priceMultiplier())));
        yield "dividend";
      }
    };
    return new Notice("corporate-actions." + key, values);
  }

  private static String split(Split split, Map<String, String> values) {
    values.put("<symbol>", split.symbol());
    values.put("<new-symbol>", split.resultSymbol());
    values.put("<old-rate>", plain(split.oldRate()));
    values.put("<new-rate>", plain(split.newRate()));
    boolean renamed = !split.resultSymbol().equals(split.symbol());
    String kind = split.newRate().compareTo(split.oldRate()) >= 0 ? "split" : "reverse-split";
    return renamed ? kind + "-renamed" : kind;
  }

  private static String merger(Merger merger, Map<String, String> values, TradeRules rules,
      Formats formats) {
    values.put("<symbol>", merger.acquiree());
    values.put("<cash-per-share>", formats.money(merger.cashPerShare() == null ? BigDecimal.ZERO
        : merger.cashPerShare().multiply(rules.priceMultiplier())));
    if (!merger.hasStock()) {
      return "buyout";
    }
    values.put("<new-symbol>", merger.acquirer());
    values.put("<ratio>", plain(merger.acquirerRate().divide(merger.acquireeRate(), 6,
        RoundingMode.HALF_UP)));
    return "stock-merger";
  }

  private static String plain(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }
}
