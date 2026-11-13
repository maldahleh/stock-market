package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.events.StockPurchaseEvent;
import com.maldahleh.stockmarket.events.StockSaleEvent;
import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.market.Quote;
import com.maldahleh.stockmarket.menu.ItemTemplate;
import com.maldahleh.stockmarket.menu.MenuHolder;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.trade.TradeResult;
import com.maldahleh.stockmarket.trade.TradeResult.Failure;
import com.maldahleh.stockmarket.trade.TradeResult.Success;
import com.maldahleh.stockmarket.trade.TradeService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Runs trades for players and tells them how each went. Large trades and selling everything
 * ask for a confirmation first: a menu when the trade came from a menu click, or a clickable
 * chat message when it came from a command.
 */
public class Trader {

  private static final Duration CONFIRM_WINDOW = Duration.ofSeconds(30);

  private record Pending(String symbol, long shares, boolean buying, Instant expires) {}

  private final TradeService trades;
  private final MarketData market;
  private final Portfolios portfolios;
  private final Messages messages;
  private final Formats formats;
  private final BigDecimal confirmAbove;
  private final Executor serverThread;
  private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

  public Trader(TradeService trades, MarketData market, Portfolios portfolios, Messages messages,
      Formats formats, BigDecimal confirmAbove, Executor serverThread) {
    this.serverThread = serverThread;
    this.trades = trades;
    this.market = market;
    this.portfolios = portfolios;
    this.messages = messages;
    this.formats = formats;
    this.confirmAbove = confirmAbove;
  }

  public void buy(Player player, String symbol, long shares, boolean fromMenu) {
    request(player, symbol.toUpperCase(), shares, true, false, fromMenu);
  }

  public void sell(Player player, String symbol, long shares, boolean fromMenu) {
    request(player, symbol.toUpperCase(), shares, false, false, fromMenu);
  }

  /** Sells every share the player owns of the symbol. Always asks first. */
  public void sellAll(Player player, String symbol, boolean fromMenu) {
    String upper = symbol.toUpperCase();
    long owned = portfolios.get(player.getUniqueId(), upper).map(Holding::shares).orElse(0L);
    if (owned == 0) {
      messages.send(player, "failures.INSUFFICIENT_SHARES", Map.of("<symbol>", upper));
      return;
    }
    request(player, upper, owned, false, true, fromMenu);
  }

  /** Runs the trade waiting for confirmation, if any. */
  public void confirm(Player player) {
    Pending trade = pending.remove(player.getUniqueId());
    if (trade == null || trade.expires().isBefore(Instant.now())) {
      messages.send(player, "nothing-to-confirm");
      return;
    }
    execute(player, trade.symbol(), trade.shares(), trade.buying());
  }

  private void request(Player player, String symbol, long shares, boolean buying,
      boolean always, boolean fromMenu) {
    if (!always && (confirmAbove.signum() <= 0 || shares <= 0)) {
      execute(player, symbol, shares, buying);
      return;
    }
    market.quotes(List.of(symbol)).exceptionally(e -> Map.of()).thenAcceptAsync(quotes -> {
      Quote quote = quotes.get(symbol);
      BigDecimal estimate = quote == null ? null : trades.rules().serverPrice(quote.price())
          .multiply(BigDecimal.valueOf(shares));
      boolean large = confirmAbove.signum() > 0 && estimate != null
          && estimate.compareTo(confirmAbove) >= 0;
      if (!player.isOnline()) {
        return;
      }
      if (!always && !large) {
        execute(player, symbol, shares, buying);
      } else if (fromMenu) {
        openConfirm(player, symbol, shares, buying, estimate);
      } else {
        askInChat(player, symbol, shares, buying, estimate);
      }
    }, serverThread);
  }

  private Map<String, String> summary(String symbol, long shares, boolean buying,
      BigDecimal estimate) {
    return Map.of(
        "<action>", messages.raw(buying ? "confirm.buy" : "confirm.sell"),
        "<quantity>", formats.number(shares),
        "<symbol>", symbol,
        "<estimate>", estimate == null ? formats.unknown() : formats.money(estimate));
  }

  private void openConfirm(Player player, String symbol, long shares, boolean buying,
      BigDecimal estimate) {
    Map<String, String> values = summary(symbol, shares, buying, estimate);
    MenuHolder menu = new MenuHolder(9, Text.render(messages.raw("confirm.title"), values));
    List<String> lore = messages.lines("confirm.details");
    menu.set(2, new ItemTemplate(Material.LIME_CONCRETE, messages.raw("confirm.yes"), lore, null,
        false).build(values), click -> {
          player.closeInventory();
          execute(player, symbol, shares, buying);
        });
    menu.set(6, new ItemTemplate(Material.RED_CONCRETE, messages.raw("confirm.no"), List.of(),
        null, false).build(values), click -> player.closeInventory());
    player.openInventory(menu.getInventory());
  }

  private void askInChat(Player player, String symbol, long shares, boolean buying,
      BigDecimal estimate) {
    pending.put(player.getUniqueId(),
        new Pending(symbol, shares, buying, Instant.now().plus(CONFIRM_WINDOW)));
    Map<String, String> values = summary(symbol, shares, buying, estimate);
    messages.send(player, "confirm.chat", values);
    BaseComponent[] button = TextComponent.fromLegacyText(
        Text.render(messages.raw("confirm.button"), values));
    for (BaseComponent part : button) {
      part.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/stockmarket confirm"));
    }
    player.spigot().sendMessage(button);
  }

  private void execute(Player player, String symbol, long shares, boolean buying) {
    messages.send(player, "pending.trade");
    (buying ? trades.buy(player.getUniqueId(), symbol, shares)
        : trades.sell(player.getUniqueId(), symbol, shares))
        .thenAccept(result -> report(player, symbol, result, buying));
  }

  /** Tells the player how a trade went and fires the trade event. Call on the server thread. */
  public void report(Player player, String symbol, TradeResult result, boolean buying) {
    if (result instanceof Failure failure) {
      if (player.isOnline()) {
        messages.send(player, "failures." + failure.reason().name(),
            Map.of("<symbol>", symbol.toUpperCase()));
      }
      return;
    }
    Success success = (Success) result;
    var entry = success.entry();
    long shares = Math.abs(entry.shares());
    if (player.isOnline()) {
      messages.sendBlock(player, buying ? "bought" : "sold", values(success));
    }
    Bukkit.getPluginManager().callEvent(buying
        ? new StockPurchaseEvent(player, entry.symbol(), shares, entry.price(), success.fee(),
            success.total())
        : new StockSaleEvent(player, entry.symbol(), shares, entry.price(), success.fee(),
            success.total()));
  }

  /** Placeholders describing a completed trade. */
  public Map<String, String> values(Success success) {
    var entry = success.entry();
    Map<String, String> values = new HashMap<>();
    values.put("<quantity>", formats.number(Math.abs(entry.shares())));
    values.put("<symbol>", entry.symbol());
    values.put("<name>", success.asset().name() == null ? entry.symbol() : success.asset().name());
    values.put("<price>", formats.money(entry.price()));
    values.put("<value>", formats.money(success.value()));
    values.put("<fees>", formats.money(success.fee()));
    values.put("<total>", formats.money(success.total()));
    values.put("<net>", formats.signedMoney(success.net()));
    return values;
  }
}
