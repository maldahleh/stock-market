package com.maldahleh.stockmarket.command;

import com.maldahleh.stockmarket.StockMarketPlugin;
import com.maldahleh.stockmarket.bukkit.Brokers;
import com.maldahleh.stockmarket.bukkit.Formats;
import com.maldahleh.stockmarket.orders.Order;
import com.maldahleh.stockmarket.orders.OrderBook;
import com.maldahleh.stockmarket.orders.OrderBook.Placement;
import com.maldahleh.stockmarket.orders.OrderType;
import java.math.BigDecimal;
import com.maldahleh.stockmarket.bukkit.Messages;
import com.maldahleh.stockmarket.bukkit.Text;
import com.maldahleh.stockmarket.bukkit.Trader;
import com.maldahleh.stockmarket.market.Quantities;
import com.maldahleh.stockmarket.menu.Menus;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
      "orders", "spawnbroker", "removebroker", "reload");
  private static final int MAX_COMPARE = 3;
  private static final double BROKER_REMOVE_RADIUS = 5;

  private final Supplier<Services> services;
  private final Messages messages;
  private final boolean commandsDisabled;
  private final Runnable reload;

  public StockCommand(Supplier<Services> services, Messages messages, boolean commandsDisabled,
      Runnable reload) {
    this.services = services;
    this.messages = messages;
    this.commandsDisabled = commandsDisabled;
    this.reload = reload;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String sub = subcommand(args);
    String refusal = refusal(sender, sub);
    if (refusal != null) {
      messages.send(sender, refusal);
      return true;
    }
    if (sub.equals("help")) {
      help(sender);
    } else if (sub.equals("reload")) {
      reload.run();
      messages.send(sender, "reloaded");
    } else {
      runAsPlayer(sender, sub, Arrays.copyOfRange(args, Math.min(1, args.length), args.length));
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
      return "invalid-syntax";
    }
    return sender.hasPermission(permission(sub)) ? null : "no-permission";
  }

  private void runAsPlayer(CommandSender sender, String sub, String[] args) {
    if (!(sender instanceof Player player)) {
      messages.send(sender, "player-only");
      return;
    }
    boolean brokerCommand = sub.equals("spawnbroker") || sub.equals("removebroker");
    if (commandsDisabled && !brokerCommand
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
          messages.send(player, "invalid-syntax");
          return;
        }
        s.menus().openLookup(player, args[0]);
      }
      case "compare" -> {
        List<String> symbols = args.length == 0 ? List.of()
            : Arrays.stream(String.join(",", args).split(",")).map(String::trim)
                .filter(x -> !x.isEmpty()).toList();
        if (symbols.isEmpty()) {
          messages.send(player, "invalid-syntax");
        } else if (symbols.size() > MAX_COMPARE) {
          messages.send(player, "compare-max");
        } else {
          s.menus().openCompare(player, symbols);
        }
      }
      case "portfolio", "transactions" -> {
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
      case "history" -> s.menus().openHistory(player, args.length == 0 ? null : args[0]);
      case "buy", "sell" -> {
        if (args.length < 1 || args.length > 2) {
          messages.send(player, "invalid-syntax");
          return;
        }
        if (sub.equals("sell") && args.length == 2 && args[1].equalsIgnoreCase("all")) {
          s.trader().sellAll(player, args[0], false);
          return;
        }
        long shares;
        try {
          shares = Quantities.parse(args[0].toUpperCase(), args.length == 2 ? args[1] : "1");
        } catch (NumberFormatException e) {
          messages.send(player, "invalid-quantity");
          return;
        }
        if (sub.equals("buy")) {
          s.trader().buy(player, args[0], shares, false);
        } else {
          s.trader().sell(player, args[0], shares, false);
        }
      }
      case "confirm" -> s.trader().confirm(player);
      case "orders" -> listOrders(player, s);
      case "order" -> order(player, args, s);
      case "spawnbroker" -> {
        s.brokers().spawn(player.getLocation());
        messages.send(player, "broker-spawned");
      }
      case "removebroker" -> messages.send(player,
          s.brokers().removeNearest(player.getLocation(), BROKER_REMOVE_RADIUS)
              ? "broker-removed" : "no-broker-nearby");
      default -> messages.send(player, "invalid-syntax");
    }
  }

  private record OrderRequest(OrderType type, String symbol, long shares, BigDecimal target) {}

  private void order(Player player, String[] args, Services s) {
    if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
      listOrders(player, s);
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
    messages.send(player, s.orders().cancel(player.getUniqueId(), id)
        ? "orders.cancelled-by-you" : "orders.not-found", Map.of("<id>", String.valueOf(id)));
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
      return type == null ? null : new OrderRequest(type, args[1],
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
      messages.send(player, "orders.placed." + placed.order().type().name(),
          StockMarketPlugin.orderValues(placed.order(), s.formats()));
    } else {
      messages.send(player, "orders.problems." + ((Placement.Rejected) placement).problem().name(),
          Map.of("<symbol>", request.symbol().toUpperCase()));
    }
  }

  /** Lists the player's open orders in chat; used by the list menu too. */
  public void showOrders(Player player) {
    Services s = services.get();
    if (s == null) {
      messages.send(player, "not-configured");
    } else {
      listOrders(player, s);
    }
  }

  private void listOrders(Player player, Services s) {
    List<Order> open = s.orders().of(player.getUniqueId());
    if (open.isEmpty()) {
      messages.send(player, "orders.none");
      return;
    }
    messages.send(player, "orders.header");
    for (Order order : open) {
      messages.sendBlock(player, "orders.line." + order.type().name(),
          StockMarketPlugin.orderValues(order, s.formats()));
    }
  }

  /** The player whose data to show: yourself, or someone else with the .other permission. */
  @SuppressWarnings("deprecation")
  private OfflinePlayer target(Player player, String sub, String[] args) {
    if (args.length == 0) {
      return player;
    }
    if (!player.hasPermission("stockmarket." + sub + ".other")) {
      messages.send(player, "no-permission");
      return null;
    }
    OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
    if (!target.hasPlayedBefore() && !target.isOnline()) {
      messages.send(player, "player-not-found", Map.of("<player>", args[0]));
      return null;
    }
    return target;
  }

  private void help(CommandSender sender) {
    List<String> lines = new ArrayList<>();
    for (String sub : SUBCOMMANDS) {
      if (sender.hasPermission(permission(sub))) {
        lines.add(messages.raw("commands." + sub));
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
      case "help" -> "stockmarket.use";
      case "spawnbroker", "removebroker" -> "stockmarket.spawnbroker";
      case "confirm" -> "stockmarket.use";
      case "order", "orders" -> "stockmarket.orders";
      default -> "stockmarket." + sub;
    };
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias,
      String[] args) {
    if (args.length == 1) {
      return SUBCOMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase()))
          .filter(s -> sender.hasPermission(permission(s))).toList();
    }
    Services s = services.get();
    if (args.length == 2 && s != null && sender instanceof Player player
        && (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("lookup"))) {
      return s.portfolios().of(player.getUniqueId()).stream().map(Holding::symbol)
          .filter(x -> x.startsWith(args[1].toUpperCase())).toList();
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("order")) {
      return List.of("buy", "sell", "stop", "cancel", "list").stream()
          .filter(x -> x.startsWith(args[1].toLowerCase())).toList();
    }
    if (args.length == 2 && (args[0].equalsIgnoreCase("portfolio")
        || args[0].equalsIgnoreCase("transactions"))) {
      return null;
    }
    return List.of();
  }

  /** Null until the plugin has a working Alpaca key. */
  public record Services(Menus menus, Trader trader, Brokers brokers, Portfolios portfolios,
      OrderBook orders, Formats formats) {}
}
