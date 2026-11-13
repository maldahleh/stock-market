package com.maldahleh.stockmarket.command;

import com.maldahleh.stockmarket.StockMarketPlugin;
import com.maldahleh.stockmarket.bukkit.Brokers;
import com.maldahleh.stockmarket.bukkit.Formats;
import com.maldahleh.stockmarket.bukkit.Messages;
import com.maldahleh.stockmarket.bukkit.Text;
import com.maldahleh.stockmarket.bukkit.Trader;
import com.maldahleh.stockmarket.market.Quantities;
import com.maldahleh.stockmarket.menu.Menus;
import com.maldahleh.stockmarket.orders.Order;
import com.maldahleh.stockmarket.orders.OrderBook;
import com.maldahleh.stockmarket.orders.OrderBook.Placement;
import com.maldahleh.stockmarket.orders.OrderType;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /stockmarket and its subcommands. */
public class StockCommand implements TabExecutor {

  private static final List<String> SUBCOMMANDS = List.of("help", "list", "tutorial", "lookup",
      "compare", "portfolio", "transactions", "history", "buy", "sell", "confirm", "order",
      "orders", "spawnbroker", "removebroker", "status", "reload");
  private static final Set<String> CONSOLE_REPORTS = Set.of("portfolio", "transactions",
      "orders");
  private static final int MAX_COMPARE = 3;
  private static final double BROKER_REMOVE_RADIUS = 5;

  private final Supplier<Services> services;
  private final Messages messages;
  private final Supplier<Boolean> commandsDisabled;
  private final Admin admin;

  public StockCommand(Supplier<Services> services, Messages messages,
      Supplier<Boolean> commandsDisabled, Admin admin) {
    this.services = services;
    this.messages = messages;
    this.commandsDisabled = commandsDisabled;
    this.admin = admin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String sub = subcommand(args);
    String[] rest = Arrays.copyOfRange(args, Math.min(1, args.length), args.length);
    String refusal = refusal(sender, sub);
    if (refusal != null) {
      messages.send(sender, refusal);
    } else if (sub.equals("help")) {
      help(sender);
    } else if (sub.equals("reload")) {
      admin.reload().accept(sender);
    } else if (sub.equals("status")) {
      admin.status().accept(sender);
    } else if (sender instanceof Player player) {
      runAsPlayer(player, sub, rest);
    } else if (CONSOLE_REPORTS.contains(sub) && rest.length == 1) {
      consoleReport(sender, sub, rest[0]);
    } else {
      messages.send(sender, "player-only");
    }
    return true;
  }

  private static String subcommand(String[] args) {
    String sub = args.length == 0 ? "help" : args[0].toLowerCase();
    return sub.equals("simplebroker") ? "spawnbroker" : sub;
  }

  /** The message key explaining why the sender can't run this, or null if they can. */
  private String refusal(CommandSender sender, String sub) {
    if (!SUBCOMMANDS.contains(sub)) {
      return "unknown-command";
    }
    return sender.hasPermission(permission(sub)) ? null : "no-permission";
  }

  private void runAsPlayer(Player player, String sub, String[] args) {
    boolean brokerCommand = sub.equals("spawnbroker") || sub.equals("removebroker");
    if (commandsDisabled.get() && !brokerCommand
        && !player.hasPermission("stockmarket.commandbypass")) {
      messages.send(player, "commands-disabled");
      return;
    }
    Services s = services.get();
    if (s == null) {
      messages.send(player, "not-configured");
      return;
    }
    run(player, sub, args, s);
  }

  private void run(Player player, String sub, String[] args, Services s) {
    switch (sub) {
      case "list" -> s.menus().openList(player);
      case "tutorial" -> s.menus().openTutorial(player);
      case "lookup" -> {
        if (args.length != 1) {
          usage(player, sub);
        } else {
          s.menus().openLookup(player, args[0]);
        }
      }
      case "compare" -> compare(player, args, s);
      case "portfolio", "transactions" -> showAccount(player, sub, args, s);
      case "history" -> s.menus().openHistory(player, args.length == 0 ? null : args[0]);
      case "buy", "sell" -> trade(player, sub, args, s);
      case "confirm" -> s.trader().confirm(player);
      case "orders" -> showOrders(player, args, s);
      case "order" -> order(player, args, s);
      case "spawnbroker" -> {
        s.brokers().spawn(player.getLocation());
        messages.send(player, "broker-spawned");
      }
      case "removebroker" -> messages.send(player,
          s.brokers().removeNearest(player.getLocation(), BROKER_REMOVE_RADIUS)
              ? "broker-removed" : "no-broker-nearby");
      default -> usage(player, sub);
    }
  }

  /** "Try: /stocks buy <symbol> [shares]" for a known subcommand used the wrong way. */
  private void usage(CommandSender sender, String sub) {
    messages.send(sender, "usage", Map.of("<usage>", messages.raw("commands." + sub)));
  }

  private void compare(Player player, String[] args, Services s) {
    List<String> symbols = Arrays.stream(String.join(",", args).split(",")).map(String::trim)
        .filter(x -> !x.isEmpty()).toList();
    if (symbols.isEmpty()) {
      usage(player, "compare");
    } else if (symbols.size() > MAX_COMPARE) {
      messages.send(player, "compare-max");
    } else {
      s.menus().openCompare(player, symbols);
    }
  }

  private void showAccount(Player player, String sub, String[] args, Services s) {
    OfflinePlayer target = target(player, sub, args);
    if (target == null) {
      return;
    }
    if (sub.equals("portfolio")) {
      s.menus().openPortfolio(player, target);
    } else {
      s.menus().openTransactions(player, target);
    }
  }

  private void trade(Player player, String sub, String[] args, Services s) {
    if (args.length < 1 || args.length > 2) {
      usage(player, sub);
      return;
    }
    String symbol = args[0].toUpperCase();
    if (sub.equals("sell") && args.length == 2 && args[1].equalsIgnoreCase("all")) {
      s.trader().sellAll(player, symbol, null);
      return;
    }
    long units;
    try {
      units = Quantities.parse(symbol, args.length == 2 ? args[1] : "1");
    } catch (NumberFormatException e) {
      messages.send(player, "failures.INVALID_QUANTITY");
      return;
    }
    if (sub.equals("buy")) {
      s.trader().buy(player, symbol, units, null);
    } else {
      s.trader().sell(player, symbol, units, null);
    }
  }

  // Orders

  private record OrderRequest(OrderType type, String symbol, long shares, BigDecimal target) {}

  private void order(Player player, String[] args, Services s) {
    if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
      listOrders(player, player.getUniqueId(), s);
      return;
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("cancel")) {
      cancelOrder(player, args[1], s);
      return;
    }
    OrderRequest request = parseOrder(args);
    if (request == null) {
      messages.send(player, "orders.usage");
      return;
    }
    s.orders().place(player.getUniqueId(), request.type(), request.symbol(), request.shares(),
        request.target()).thenAccept(placement -> reportPlacement(player, request, placement, s));
  }

  private void cancelOrder(Player player, String rawId, Services s) {
    long id;
    try {
      id = Long.parseLong(rawId.replace("#", ""));
    } catch (NumberFormatException e) {
      messages.send(player, "orders.usage");
      return;
    }
    boolean cancelled = s.orders().cancel(player.getUniqueId(), id)
        || player.hasPermission("stockmarket.orders.other") && s.orders().cancelAny(id);
    messages.send(player, cancelled ? "orders.cancelled-by-you" : "orders.not-found",
        Map.of("<id>", String.valueOf(id)));
  }

  /** Reads "buy|sell|stop <symbol> <shares> <price>", or returns null if it doesn't parse. */
  private static OrderRequest parseOrder(String[] args) {
    if (args.length != 4) {
      return null;
    }
    OrderType type = switch (args[0].toLowerCase()) {
      case "buy" -> OrderType.LIMIT_BUY;
      case "sell" -> OrderType.LIMIT_SELL;
      case "stop" -> OrderType.STOP_SELL;
      default -> null;
    };
    try {
      return type == null ? null : new OrderRequest(type, args[1].toUpperCase(),
          Quantities.parse(args[1].toUpperCase(), args[2]),
          new BigDecimal(args[3].replace("$", "").replace(",", "")));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private void reportPlacement(Player player, OrderRequest request, Placement placement,
      Services s) {
    if (!player.isOnline()) {
      return;
    }
    if (placement instanceof Placement.Placed placed) {
      reportPlaced(player, placed, s);
      return;
    }
    String problem = ((Placement.Rejected) placement).problem().name();
    String key = messages.has("orders.problems." + problem) ? "orders.problems." + problem
        : "failures." + problem;
    messages.send(player, key, Map.of(
        "<symbol>", request.symbol(),
        "<owned>", s.formats().quantity(request.symbol(), s.portfolios()
            .get(player.getUniqueId(), request.symbol()).map(Holding::shares).orElse(0L)),
        "<max>", s.formats().number(s.trader().rules().maxSharesPerTrade()),
        "<max-orders>", String.valueOf(s.orders().maxOpenPerPlayer()),
        "<minimum>", s.formats().usd(s.trader().rules().minimumPrice())));
  }

  private void reportPlaced(Player player, Placement.Placed placed, Services s) {
    Order order = placed.order();
    Map<String, String> values = new java.util.HashMap<>(
        StockMarketPlugin.orderValues(order, s.formats()));
    BigDecimal now = placed.currentPrice();
    values.put("<current>", s.formats().money(now));
    messages.send(player, "orders.placed." + order.type().name(), values);
    if (now == null) {
      return;
    }
    if (order.type().triggeredBy(now, order.target())) {
      messages.send(player, "orders.fills-now", values);
    } else if (looksLikeDollars(order.target(), now, s.trader().rules().priceMultiplier())) {
      values.put("<multiplier>", s.trader().rules().priceMultiplier().stripTrailingZeros()
          .toPlainString());
      messages.send(player, "orders.server-prices", values);
    }
  }

  /** True if the target is about the real US-dollar price, i.e. off by the multiplier. */
  private static boolean looksLikeDollars(BigDecimal target, BigDecimal now,
      BigDecimal multiplier) {
    if (multiplier.compareTo(BigDecimal.valueOf(2)) < 0) {
      return false;
    }
    BigDecimal asServer = target.multiply(multiplier);
    return asServer.compareTo(now.multiply(BigDecimal.valueOf(0.5))) > 0
        && asServer.compareTo(now.multiply(BigDecimal.valueOf(1.5))) < 0;
  }

  /** Lists the player's open orders in chat; used by the list menu too. */
  public void showOrders(Player player) {
    Services s = services.get();
    if (s == null) {
      messages.send(player, "not-configured");
    } else {
      listOrders(player, player.getUniqueId(), s);
    }
  }

  private void showOrders(Player player, String[] args, Services s) {
    if (args.length == 0) {
      listOrders(player, player.getUniqueId(), s);
      return;
    }
    OfflinePlayer target = target(player, "orders", args);
    if (target != null) {
      listOrders(player, target.getUniqueId(), s);
    }
  }

  /** One line per order, each with a clickable [Cancel]. */
  private void listOrders(CommandSender viewer, UUID owner, Services s) {
    List<Order> open = s.orders().of(owner);
    if (open.isEmpty()) {
      messages.send(viewer, "orders.none");
      return;
    }
    messages.send(viewer, "orders.header");
    for (Order order : open) {
      Map<String, String> values = StockMarketPlugin.orderValues(order, s.formats());
      messages.sendBlock(viewer, "orders.line." + order.type().name(), values);
      if (viewer instanceof Player player) {
        player.spigot().sendMessage(Trader.button(messages.raw("orders.cancel-button"), values,
            "/stockmarket order cancel " + order.id()));
      }
    }
  }

  // Other players and the console

  /** The player whose data to show: yourself, or someone else with the .other permission. */
  private OfflinePlayer target(Player player, String sub, String[] args) {
    if (args.length == 0) {
      return player;
    }
    if (!player.hasPermission("stockmarket." + sub + ".other")) {
      messages.send(player, "no-permission");
      return null;
    }
    return find(player, args[0]);
  }

  @SuppressWarnings("deprecation")
  private OfflinePlayer find(CommandSender sender, String name) {
    OfflinePlayer target = Bukkit.getOfflinePlayer(name);
    if (!target.hasPlayedBefore() && !target.isOnline()) {
      messages.send(sender, "player-not-found", Map.of("<player>", name));
      return null;
    }
    return target;
  }

  /** Portfolio, trades or orders printed as text, for the console. */
  private void consoleReport(CommandSender sender, String sub, String name) {
    Services s = services.get();
    if (s == null) {
      messages.send(sender, "not-configured");
      return;
    }
    OfflinePlayer target = find(sender, name);
    if (target == null) {
      return;
    }
    switch (sub) {
      case "portfolio" -> s.menus().printPortfolio(sender, target);
      case "transactions" -> s.menus().printTransactions(sender, target);
      default -> listOrders(sender, target.getUniqueId(), s);
    }
  }

  private void help(CommandSender sender) {
    List<String> lines = new ArrayList<>();
    for (String sub : SUBCOMMANDS) {
      if (sender.hasPermission(permission(sub)) && !messages.raw("commands." + sub).isBlank()) {
        lines.addAll(messages.lines("commands." + sub));
      }
    }
    for (String line : messages.lines("help")) {
      if (line.contains("<commands>")) {
        lines.forEach(l -> sender.sendMessage(Text.color(l)));
      } else {
        sender.sendMessage(Text.color(line));
      }
    }
  }

  private static String permission(String sub) {
    return switch (sub) {
      case "help", "confirm" -> "stockmarket.use";
      case "spawnbroker", "removebroker" -> "stockmarket.spawnbroker";
      case "order", "orders" -> "stockmarket.orders";
      case "status" -> "stockmarket.reload";
      default -> "stockmarket." + sub;
    };
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias,
      String[] args) {
    if (args.length == 1) {
      return matching(SUBCOMMANDS.stream().filter(s -> sender.hasPermission(permission(s)))
          .toList(), args[0]);
    }
    Services s = services.get();
    if (s == null || !(sender instanceof Player player)) {
      return null;
    }
    String sub = args[0].toLowerCase();
    if (args.length == 2) {
      return completeSecond(sub, args[1], s, player.getUniqueId());
    }
    if (args.length == 3 && sub.equals("order")) {
      return completeOrder(args[1].toLowerCase(), args[2], s, player.getUniqueId());
    }
    return List.of();
  }

  private static List<String> completeSecond(String sub, String typed, Services s, UUID id) {
    return switch (sub) {
      case "sell" -> matching(held(s, id), typed);
      case "buy", "lookup", "history" -> matching(knownSymbols(s, id), typed);
      case "order" -> matching(List.of("buy", "sell", "stop", "cancel", "list"), typed);
      case "portfolio", "transactions", "orders" -> null;
      default -> List.of();
    };
  }

  /** The third word of /stocks order: an order id to cancel, or a symbol. */
  private static List<String> completeOrder(String action, String typed, Services s, UUID id) {
    if (action.equals("cancel")) {
      return matching(s.orders().of(id).stream().map(o -> String.valueOf(o.id())).toList(),
          typed);
    }
    return matching(action.equals("buy") ? knownSymbols(s, id) : held(s, id), typed);
  }

  private static List<String> held(Services s, UUID player) {
    return s.portfolios().of(player).stream().map(Holding::symbol).toList();
  }

  private static List<String> knownSymbols(Services s, UUID player) {
    Set<String> symbols = new TreeSet<>(s.menus().listSymbols());
    symbols.addAll(held(s, player));
    return new ArrayList<>(symbols);
  }

  private static List<String> matching(List<String> options, String typed) {
    String prefix = typed.toLowerCase();
    return options.stream().filter(o -> o.toLowerCase().startsWith(prefix)).toList();
  }

  /** What the plugin offers once a data source is connected; null until then. */
  public record Services(Menus menus, Trader trader, Brokers brokers, Portfolios portfolios,
      OrderBook orders, Formats formats) {}

  /** Owner commands that work even when the market isn't connected. */
  public record Admin(Consumer<CommandSender> reload, Consumer<CommandSender> status) {}
}
