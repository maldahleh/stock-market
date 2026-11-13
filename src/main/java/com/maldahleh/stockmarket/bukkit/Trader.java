package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.events.StockPurchaseEvent;
import com.maldahleh.stockmarket.events.StockSaleEvent;
import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.market.Quantities;
import com.maldahleh.stockmarket.market.Quote;
import com.maldahleh.stockmarket.menu.ItemTemplate;
import com.maldahleh.stockmarket.menu.MenuHolder;
import com.maldahleh.stockmarket.orders.Order;
import com.maldahleh.stockmarket.orders.OrderBook;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.trade.TradeResult;
import com.maldahleh.stockmarket.trade.TradeResult.Failure;
import com.maldahleh.stockmarket.trade.TradeResult.Success;
import com.maldahleh.stockmarket.trade.TradeRules;
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

  private final TradeService trades;
  private final MarketData market;
  private final Portfolios portfolios;
  private final Messages messages;
  private final Formats formats;
  private final Notices notices;
  private final Settings settings;
  private final Executor serverThread;
  private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
  private OrderBook orders;

  public Trader(TradeService trades, MarketData market, Portfolios portfolios, Messages messages,
      Formats formats, Notices notices, Settings settings, Executor serverThread) {
    this.trades = trades;
    this.market = market;
    this.portfolios = portfolios;
    this.messages = messages;
    this.formats = formats;
    this.notices = notices;
    this.settings = settings;
    this.serverThread = serverThread;
  }

  /** Lets sales warn about stop-losses and sell orders that no longer have enough shares. */
  public void watchOrders(OrderBook orderBook) {
    this.orders = orderBook;
  }

  public TradeRules rules() {
    return trades.rules();
  }

  /**
   * @param back reopens the menu the trade came from; null when it came from a command
   */
  public void buy(Player player, String symbol, long units, Runnable back) {
    request(player, symbol.toUpperCase(), units, true, false, back);
  }

  public void sell(Player player, String symbol, long units, Runnable back) {
    request(player, symbol.toUpperCase(), units, false, false, back);
  }

  /** Sells everything the player owns of the symbol. Always asks first. */
  public void sellAll(Player player, String symbol, Runnable back) {
    String upper = symbol.toUpperCase();
    long owned = portfolios.get(player.getUniqueId(), upper).map(Holding::shares).orElse(0L);
    if (owned == 0) {
      messages.send(player, "none-owned", Map.of("<symbol>", upper));
      return;
    }
    request(player, upper, owned, false, true, back);
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
      boolean always, Runnable back) {
    if (!always && (settings.confirmAbove().signum() <= 0 || shares <= 0)) {
      execute(player, symbol, shares, buying);
      return;
    }
    market.quotes(List.of(symbol)).exceptionally(e -> Map.of()).thenAcceptAsync(quotes -> {
      BigDecimal estimate = estimate(symbol, quotes.get(symbol), shares);
      // Without an estimate the trade might be large, so it asks rather than guessing.
      boolean large = estimate == null || estimate.compareTo(settings.confirmAbove()) >= 0;
      if (!player.isOnline()) {
        return;
      }
      if (!always && !large) {
        execute(player, symbol, shares, buying);
      } else if (back != null) {
        openConfirm(player, new Pending(symbol, shares, buying, null), estimate, back);
      } else {
        askInChat(player, new Pending(symbol, shares, buying, null), estimate);
      }
    }, serverThread);
  }

  private BigDecimal estimate(String symbol, Quote quote, long shares) {
    return quote == null ? null
        : Quantities.value(symbol, trades.rules().serverPrice(quote.price()), shares);
  }

  private Map<String, String> summary(Pending trade, BigDecimal estimate) {
    BigDecimal fee = estimate == null ? null : trades.rules().fee(estimate, trade.buying());
    BigDecimal total = fee == null ? null
        : trade.buying() ? estimate.add(fee) : estimate.subtract(fee);
    return Map.of(
        "<action>", messages.raw(trade.buying() ? "confirm.buy" : "confirm.sell"),
        "<quantity>", formats.quantity(trade.symbol(), trade.shares()),
        "<symbol>", trade.symbol(),
        "<estimate>", formats.money(estimate),
        "<fees>", formats.money(fee),
        "<total>", formats.money(total),
        "<direction>", messages.raw(trade.buying() ? "confirm.pay" : "confirm.receive"));
  }

  private void openConfirm(Player player, Pending trade, BigDecimal estimate, Runnable back) {
    Map<String, String> values = summary(trade, estimate);
    MenuHolder menu = new MenuHolder(9, Text.render(messages.raw("confirm.title"), values));
    List<String> lore = messages.lines("confirm.details");
    menu.set(2, new ItemTemplate(Material.LIME_CONCRETE, messages.raw("confirm.yes"), lore, null,
        false, null).build(values), click -> {
          back.run();
          execute(player, trade.symbol(), trade.shares(), trade.buying());
        });
    menu.set(6, new ItemTemplate(Material.RED_CONCRETE, messages.raw("confirm.no"), List.of(),
        null, false, null).build(values), click -> {
          messages.send(player, "confirm.cancelled");
          back.run();
        });
    player.openInventory(menu.getInventory());
  }

  private void askInChat(Player player, Pending trade, BigDecimal estimate) {
    pending.put(player.getUniqueId(), new Pending(trade.symbol(), trade.shares(), trade.buying(),
        Instant.now().plus(CONFIRM_WINDOW)));
    Map<String, String> values = summary(trade, estimate);
    messages.send(player, "confirm.chat", values);
    player.spigot().sendMessage(button(messages.raw("confirm.button"), values,
        "/stockmarket confirm"));
  }

  /** A chat line that runs {@code command} when clicked. */
  public static BaseComponent[] button(String text, Map<String, String> values, String command) {
    BaseComponent[] parts = TextComponent.fromLegacyText(Text.render(text, values));
    for (BaseComponent part : parts) {
      part.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
    }
    return parts;
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
            failureValues(player.getUniqueId(), symbol.toUpperCase(), failure));
      }
      return;
    }
    announce(player.getUniqueId(), (Success) result, buying);
    if (!buying) {
      warnShortOrders(player, symbol.toUpperCase());
    }
  }

  private Map<String, String> failureValues(UUID player, String symbol, Failure failure) {
    TradeRules rules = trades.rules();
    long owned = portfolios.get(player, symbol).map(Holding::shares).orElse(0L);
    Map<String, String> values = new HashMap<>();
    values.put("<symbol>", symbol);
    values.put("<reopens>", failure.when() == null ? messages.raw("reopens-soon")
        : formats.until(failure.when()));
    values.put("<cost>", formats.money(failure.amount()));
    values.put("<balance>", formats.balance(player));
    values.put("<owned>", formats.quantity(symbol, owned));
    values.put("<max>", formats.number(rules.maxSharesPerTrade()));
    values.put("<minimum>", formats.usd(rules.minimumPrice()));
    return values;
  }

  /**
   * Tells the owner about a completed trade (saved for later if they're offline), refreshes an
   * open menu, announces big trades, and fires the trade event.
   */
  public void announce(UUID owner, Success success, boolean buying) {
    Map<String, String> values = values(success);
    notices.tell(owner, buying ? "bought" : "sold", values, true);
    broadcastIfLarge(owner, success, buying, values);
    Player player = Bukkit.getPlayer(owner);
    if (player == null) {
      return;
    }
    if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder open) {
      open.refresh();
    }
    var entry = success.entry();
    long shares = Math.abs(entry.shares());
    Bukkit.getPluginManager().callEvent(buying
        ? new StockPurchaseEvent(player, entry.symbol(), shares, entry.price(), success.fee(),
            success.total())
        : new StockSaleEvent(player, entry.symbol(), shares, entry.price(), success.fee(),
            success.total()));
  }

  private void broadcastIfLarge(UUID owner, Success success, boolean buying,
      Map<String, String> values) {
    BigDecimal threshold = settings.broadcastAbove();
    if (threshold.signum() <= 0 || success.value().compareTo(threshold) < 0) {
      return;
    }
    Map<String, String> withName = new HashMap<>(values);
    String name = Bukkit.getOfflinePlayer(owner).getName();
    withName.put("<player>", name == null ? "Someone" : name);
    withName.put("<action>", messages.raw(buying ? "broadcast.bought" : "broadcast.sold"));
    messages.render("broadcast.big-trade", withName, true).forEach(Bukkit::broadcastMessage);
  }

  /** After a sale, points out sell and stop orders that now need more shares than are left. */
  private void warnShortOrders(Player player, String symbol) {
    if (orders == null) {
      return;
    }
    long left = portfolios.get(player.getUniqueId(), symbol).map(Holding::shares).orElse(0L);
    for (Order order : orders.of(player.getUniqueId())) {
      if (!order.type().buying() && order.symbol().equals(symbol) && order.shares() > left) {
        messages.send(player, "orders.short", Map.of(
            "<id>", String.valueOf(order.id()),
            "<quantity>", formats.quantity(symbol, order.shares()),
            "<owned>", formats.quantity(symbol, left),
            "<symbol>", symbol));
      }
    }
  }

  /** Placeholders describing a completed trade. */
  public Map<String, String> values(Success success) {
    var entry = success.entry();
    Map<String, String> values = new HashMap<>();
    values.put("<quantity>", formats.quantity(entry.symbol(), Math.abs(entry.shares())));
    values.put("<symbol>", entry.symbol());
    values.put("<name>", success.asset().name() == null ? entry.symbol() : success.asset().name());
    values.put("<price>", formats.money(entry.price()));
    values.put("<value>", formats.money(success.value()));
    values.put("<fees>", formats.money(success.fee()));
    values.put("<total>", formats.money(success.total()));
    values.put("<net>", formats.signedMoney(success.net()));
    return values;
  }

  /**
   * @param confirmAbove trades worth at least this ask first; 0 turns it off
   * @param broadcastAbove trades worth at least this are announced to everyone; 0 turns it off
   */
  public record Settings(BigDecimal confirmAbove, BigDecimal broadcastAbove) {}

  private record Pending(String symbol, long shares, boolean buying, Instant expires) {}
}
