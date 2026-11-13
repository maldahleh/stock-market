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

  /** Null until the plugin has a working Alpaca key. */
  public record Services(Menus menus, Trader trader, Brokers brokers, Portfolios portfolios,
      OrderBook orders, Formats formats) {}

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
    String sub = args.length == 0 ? "help" : args[0].toLowerCase();
    if (sub.equals("simplebroker")) {
      sub = "spawnbroker";
    }
    if (!SUBCOMMANDS.contains(sub)) {
      messages.send(sender, "invalid-syntax");
      return true;
    }
    if (!sender.hasPermission(permission(sub))) {
      messages.send(sender, "no-permission");
      return true;
    }
    if (sub.equals("help")) {
      help(sender);
      return true;
    }
    if (sub.equals("reload")) {
      reload.run();
      messages.send(sender, "reloaded");
      return true;
    }
    if (!(sender instanceof Player player)) {
      messages.send(sender, "player-only");
      return true;
    }
    if (commandsDisabled && !sub.equals("spawnbroker") && !sub.equals("removebroker")
        && !player.hasPermission("stockmarket.commandbypass")) {
      messages.send(player, "commands-disabled");
      return true;
    }
    Services s = services.get();
    if (s == null) {
      messages.send(player, "not-configured");
      return true;
    }
    run(player, sub, Arrays.copyOfRange(args, 1, args.length), s);
    return true;
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
          shares = args.length == 2 ? Long.parseLong(args[1]) : 1;
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

  private void order(Player player, String[] args, Services s) {
    if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
      listOrders(player, s);
      return;
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("cancel")) {
      long id;
      try {
        id = Long.parseLong(args[1].replace("#", ""));
      } catch (NumberFormatException e) {
        messages.send(player, "orders.usage");
        return;
      }
      messages.send(player, s.orders().cancel(player.getUniqueId(), id)
          ? "orders.cancelled-by-you" : "orders.not-found", Map.of("<id>", String.valueOf(id)));
      return;
    }
    if (args.length != 4) {
      messages.send(player, "orders.usage");
      return;
    }
    OrderType type = switch (args[0].toLowerCase()) {
      case "buy" -> OrderType.LIMIT_BUY;
      case "sell" -> OrderType.LIMIT_SELL;
      case "stop" -> OrderType.STOP_SELL;
      default -> null;
    };
    long shares;
    BigDecimal target;
    try {
      shares = Long.parseLong(args[2]);
      target = new BigDecimal(args[3].replace("$", "").replace(",", ""));
    } catch (NumberFormatException e) {
      type = null;
      shares = 0;
      target = null;
    }
    if (type == null) {
      messages.send(player, "orders.usage");
      return;
    }
    s.orders().place(player.getUniqueId(), type, args[1], shares, target)
        .thenAccept(placement -> {
          if (!player.isOnline()) {
            return;
          }
          if (placement instanceof Placement.Placed placed) {
            messages.send(player, "orders.placed." + placed.order().type().name(),
                StockMarketPlugin.orderValues(placed.order(), s.formats()));
          } else {
            messages.send(player, "orders.problems."
                + ((Placement.Rejected) placement).problem().name(),
                Map.of("<symbol>", args[1].toUpperCase()));
          }
        });
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
}
