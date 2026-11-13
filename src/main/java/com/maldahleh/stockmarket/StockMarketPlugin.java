package com.maldahleh.stockmarket;

import com.maldahleh.stockmarket.actions.ActionService;
import com.maldahleh.stockmarket.actions.CorporateAction;
import com.maldahleh.stockmarket.bukkit.ActionNotices;
import com.maldahleh.stockmarket.bukkit.Brokers;
import com.maldahleh.stockmarket.bukkit.Formats;
import com.maldahleh.stockmarket.bukkit.Health;
import com.maldahleh.stockmarket.bukkit.JoinSummary;
import com.maldahleh.stockmarket.bukkit.Leaderboard;
import com.maldahleh.stockmarket.bukkit.MarketNews;
import com.maldahleh.stockmarket.bukkit.Messages;
import com.maldahleh.stockmarket.bukkit.Notices;
import com.maldahleh.stockmarket.bukkit.StockExpansion;
import com.maldahleh.stockmarket.bukkit.Trader;
import com.maldahleh.stockmarket.bukkit.VaultWallet;
import com.maldahleh.stockmarket.command.StockCommand;
import com.maldahleh.stockmarket.command.StockCommand.Services;
import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.market.MarketDataProvider;
import com.maldahleh.stockmarket.market.alpaca.AlpacaProvider;
import com.maldahleh.stockmarket.menu.MenuListener;
import com.maldahleh.stockmarket.menu.Menus;
import com.maldahleh.stockmarket.orders.Order;
import com.maldahleh.stockmarket.orders.OrderBook;
import com.maldahleh.stockmarket.portfolio.EntryType;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.storage.Database;
import com.maldahleh.stockmarket.trade.TradeResult;
import com.maldahleh.stockmarket.trade.TradeRules;
import com.maldahleh.stockmarket.trade.TradeService;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class StockMarketPlugin extends JavaPlugin {

  private static final String[] MENU_FILES = {"list.yml", "lookup.yml", "compare.yml",
      "portfolio.yml", "transactions.yml", "history.yml", "tutorial.yml", "messages.yml"};

  private final Health health = new Health();
  private Economy economy;
  private Messages messages;
  private Brokers brokers;
  private ExecutorService network;
  private ExecutorService databaseThread;
  private Database database;
  private Menus menus;
  private MarketData market;
  private StockExpansion expansion;
  private StockCommand command;
  private volatile Services services;

  @Override
  public void onEnable() {
    saveFiles();
    getLogger().addHandler(health);
    messages = loadMessages();
    economy = economy();
    if (economy == null) {
      getLogger().severe("Vault and an economy plugin are required. Disabling.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    brokers = new Brokers(this, getConfig().getString("broker.name", "&8&lStock Broker"),
        () -> menus, messages);
    command = new StockCommand(() -> services, messages,
        () -> getConfig().getBoolean("broker.disable-commands"),
        new StockCommand.Admin(this::reload, this::status));
    var registered = getCommand("stockmarket");
    registered.setExecutor(command);
    registered.setTabCompleter(command);
    registerAlwaysOn();
    boot();
  }

  @Override
  public void onDisable() {
    stopServices();
    getLogger().removeHandler(health);
  }

  /** Listeners that work whether or not the market is connected. */
  private void registerAlwaysOn() {
    getServer().getPluginManager().registerEvents(brokers, this);
    getServer().getPluginManager().registerEvents(new Listener() {
      @EventHandler
      public void onJoin(PlayerJoinEvent event) {
        if (services == null && event.getPlayer().hasPermission("stockmarket.reload")) {
          messages.send(event.getPlayer(), "admin-not-configured");
        }
      }
    }, this);
  }

  /** Reads config.yml and starts trading if a key is set. */
  private void boot() {
    FileConfiguration config = getConfig();
    brokers.rename(config.getString("broker.name", "&8&lStock Broker"));
    String keyId = config.getString("alpaca.key-id", "").trim();
    String secret = config.getString("alpaca.secret", "").trim();
    if (keyId.isEmpty() || secret.isEmpty()) {
      health.connected(false, "no key in config.yml");
      getLogger().warning("No Alpaca key in config.yml, so trading is off. Add a free paper "
          + "trading key from https://alpaca.markets and run /stocks reload.");
      return;
    }
    Portfolios portfolios = openStorage(config);
    if (portfolios != null) {
      start(config, portfolios, new AlpacaProvider(keyId, secret,
          config.getString("alpaca.feed", "iex"), config.getBoolean("alpaca.live-account")));
    }
  }

  /** Re-reads every file and restarts the services, then reports how it went. */
  private void reload(CommandSender sender) {
    reloadConfig();
    messages.reload(YamlConfiguration.loadConfiguration(new File(getDataFolder(),
        "messages.yml")));
    stopServices();
    registerAlwaysOn();
    boot();
    if (services == null) {
      messages.send(sender, "reload.not-configured");
      return;
    }
    messages.send(sender, "reload.checking");
    checkConnection().thenRun(() -> Bukkit.getScheduler().runTask(this, () ->
        messages.send(sender, Boolean.TRUE.equals(health.connected()) ? "reload.connected"
            : "reload.failed", Map.of("<detail>", health.connection()))));
  }

  private void stopServices() {
    services = null;
    menus = null;
    Bukkit.getScheduler().cancelTasks(this);
    HandlerList.unregisterAll(this);
    if (expansion != null) {
      expansion.unregister();
      expansion = null;
    }
    if (network != null) {
      network.shutdownNow();
      network = null;
    }
    if (databaseThread != null) {
      databaseThread.shutdown();
      try {
        if (!databaseThread.awaitTermination(10, TimeUnit.SECONDS)) {
          getLogger().warning("Gave up waiting for database writes to finish.");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      databaseThread = null;
    }
    if (database != null) {
      database.close();
      database = null;
    }
  }

  private void saveFiles() {
    saveDefaultConfig();
    for (String file : MENU_FILES) {
      if (!new File(getDataFolder(), file).exists()) {
        saveResource(file, false);
      }
    }
  }

  /** Opens the database and loads holdings, or logs why it couldn't and returns null. */
  private Portfolios openStorage(FileConfiguration config) {
    try {
      database = openDatabase(config);
      if (database.meta(ActionService.INSTALL_DATE_KEY).isEmpty()) {
        database.setMeta(ActionService.INSTALL_DATE_KEY,
            LocalDate.now(ZoneId.of("America/New_York")).toString());
      }
      Portfolios portfolios = new Portfolios();
      portfolios.load(database.loadHoldings());
      return portfolios;
    } catch (SQLException e) {
      getLogger().log(Level.SEVERE, "Couldn't open the database or load holdings; trading is "
          + "off until it's fixed and /stocks reload is run.", e);
      health.connected(false, "database error: " + e.getMessage());
      return null;
    }
  }

  private PluginContext startThreads() {
    network = Executors.newFixedThreadPool(2, threads("StockMarket-Network"));
    databaseThread = Executors.newSingleThreadExecutor(threads("StockMarket-Database"));
    Executor serverThread = task -> {
      if (Bukkit.isPrimaryThread()) {
        task.run();
      } else if (isEnabled()) {
        Bukkit.getScheduler().runTask(this, task);
      }
    };
    return new PluginContext(serverThread, databaseThread, network, Clock.systemUTC(),
        getLogger(), id -> Bukkit.getOfflinePlayer(id).getName());
  }

  private void start(FileConfiguration config, Portfolios portfolios,
      MarketDataProvider provider) {
    PluginContext context = startThreads();
    market = new MarketData(provider, network,
        Duration.ofSeconds(Math.max(5, config.getLong("cache-seconds", 60))));
    TradeRules rules = rules(config);
    VaultWallet wallet = new VaultWallet(economy);
    Formats formats = new Formats(economy, Locale.forLanguageTag(config.getString("locale",
        "en-US")), config.getString("date-format", "yyyy-MM-dd HH:mm"),
        config.getString("unknown-data", "N/A"));

    TradeService trades = new TradeService(market, database, portfolios, wallet, rules, context);
    Notices notices = new Notices(database, messages, context);
    Trader trader = new Trader(trades, market, portfolios, messages, formats, notices,
        new Trader.Settings(decimal(config, "trading.confirm-above", "0"),
            decimal(config, "trading.broadcast-above", "0")), context.serverThread());
    menus = new Menus(getDataFolder(), market, portfolios, database, trader, formats, messages,
        new Menus.Options(Math.max(1, config.getLong("trading.bulk-amount", 5)),
            decimal(config, "trading.crypto-click-amount", "0.01"), command::showOrders),
        context);

    // The two services refer to each other, so each reaches the other through a reference.
    AtomicReference<OrderBook> orderBook = new AtomicReference<>();
    AtomicReference<ActionService> actionService = new AtomicReference<>();
    ActionService actions = new ActionService(market, database, portfolios, wallet, rules,
        config.getBoolean("dividends", true), actionNotifier(formats, notices, rules, orderBook),
        () -> orderBook.get().symbols(), context);
    actionService.set(actions);
    OrderBook orders = new OrderBook(market, database, portfolios, trades, rules,
        new OrderBook.Limits(Math.max(1, config.getInt("orders.max-open-per-player", 10)),
            Duration.ofDays(Math.max(1, config.getLong("orders.expire-days", 30)))),
        orderNotifier(formats, notices, trader),
        (symbol, after, until) -> actionService.get().changedBetween(symbol, after, until),
        context);
    loadOrders(orders);
    orderBook.set(orders);
    trader.watchOrders(orders);

    Leaderboard leaderboard = new Leaderboard(market, portfolios, rules,
        decimal(config, "leaderboard.minimum-invested", "100"));
    var events = getServer().getPluginManager();
    events.registerEvents(new MenuListener(), this);
    events.registerEvents(notices, this);
    events.registerEvents(new JoinSummary(market, portfolios, rules, messages, formats,
        context.serverThread()), this);
    if (events.isPluginEnabled("PlaceholderAPI")) {
      expansion = new StockExpansion(market, portfolios, rules, formats,
          getDescription().getVersion(), leaderboard);
      expansion.register();
    }
    MarketNews news = new MarketNews(market, messages, context.serverThread());
    schedule(config, actions, orders, leaderboard, news);
    services = new Services(menus, trader, brokers, portfolios, orders, formats);
    checkConnection();
  }

  private ActionService.Notifier actionNotifier(Formats formats, Notices notices,
      TradeRules rules, AtomicReference<OrderBook> orderBook) {
    return new ActionService.Notifier() {
      @Override
      public void notify(UUID player, CorporateAction action, BigDecimal payout) {
        ActionNotices.Notice notice = ActionNotices.of(action, payout, rules, formats, messages);
        notices.tell(player, notice.key(), notice.values(), false);
      }

      @Override
      public void applied(CorporateAction action) {
        if (!(action instanceof CorporateAction.CashDividend)) {
          orderBook.get().cancelSymbols(action.sourceSymbols(),
              OrderBook.CancelCause.CORPORATE_ACTION);
        }
      }
    };
  }

  private OrderBook.Notifier orderNotifier(Formats formats, Notices notices, Trader trader) {
    return new OrderBook.Notifier() {
      @Override
      public void filled(Order order, TradeResult.Success result) {
        notices.tell(order.owner(), "orders.filled", orderValues(order, formats), false);
        trader.announce(order.owner(), result, order.type().buying());
      }

      @Override
      public void cancelled(Order order, OrderBook.CancelCause cause) {
        Map<String, String> values = new HashMap<>(orderValues(order, formats));
        values.put("<reason>", messages.raw("orders.causes." + cause.name()));
        notices.tell(order.owner(), "orders.cancelled", values, false);
      }
    };
  }

  private void loadOrders(OrderBook orders) {
    try {
      orders.load(database.loadOrders());
    } catch (SQLException e) {
      getLogger().log(Level.SEVERE, "Couldn't load open orders; they won't fill until restart.",
          e);
    }
  }

  private void schedule(FileConfiguration config, ActionService actions, OrderBook orders,
      Leaderboard leaderboard, MarketNews news) {
    var scheduler = Bukkit.getScheduler();
    long period = Math.max(5, config.getLong("corporate-actions-check-minutes", 60)) * 60 * 20;
    scheduler.runTaskTimer(this, actions::runOnce, 20L * 30, period);
    long orderPeriod = Math.max(15, config.getLong("orders.check-seconds", 60)) * 20;
    scheduler.runTaskTimer(this, orders::check, 20L * 45, orderPeriod);
    scheduler.runTaskTimer(this, leaderboard::refresh, 20L * 60, 20L * 60 * 5);
    scheduler.runTaskTimer(this, news::check, 20L * 20, 20L * 60);
  }

  /** Checks the key now, so a typo shows up in the console rather than on a player's trade. */
  private CompletableFuture<Void> checkConnection() {
    return market.clock().handle((clock, error) -> {
      if (error != null) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        health.connected(false, cause.getMessage());
        getLogger().severe("Market data check failed: " + cause.getMessage());
      } else {
        health.connected(true, "connected");
        getLogger().info("Connected to market data. The US stock market is "
            + (clock.open() ? "open." : "closed."));
      }
      return null;
    });
  }

  /** /stocks status: whether the plugin is healthy, for owners. Works from the console. */
  private void status(CommandSender sender) {
    Map<String, String> values = new HashMap<>();
    values.put("<connection>", services == null ? messages.raw("status.off")
        : Boolean.TRUE.equals(health.connected()) ? messages.raw("status.connected")
        : messages.raw("status.failed") + " " + health.connection());
    Services current = services;
    values.put("<holders>", current == null ? "-"
        : String.valueOf(current.portfolios().owners().size()));
    values.put("<orders>", current == null ? "-" : String.valueOf(current.orders().openCount()));
    values.put("<problem>", health.lastProblem() == null ? messages.raw("status.no-problems")
        : health.lastProblem() + " (" + ago(health.lastProblemAt()) + ")");
    messages.sendBlock(sender, "status.lines", values);
    if (current != null && database != null) {
      sendMoneyTotals(sender, current.formats());
    }
  }

  private void sendMoneyTotals(CommandSender sender, Formats formats) {
    Database db = database;
    CompletableFuture.supplyAsync(() -> {
      try {
        Instant now = Instant.now();
        return new Object[] {db.moneySince(now.minus(Duration.ofDays(7))),
            db.moneySince(now.minus(Duration.ofDays(30)))};
      } catch (SQLException e) {
        throw new java.util.concurrent.CompletionException(e);
      }
    }, databaseThread).thenAccept(both -> Bukkit.getScheduler().runTask(this, () -> {
      sendTotals(sender, formats, "7", castTotals(both[0]));
      sendTotals(sender, formats, "30", castTotals(both[1]));
    })).exceptionally(error -> null);
  }

  @SuppressWarnings("unchecked")
  private static Map<EntryType, BigDecimal[]> castTotals(Object totals) {
    return (Map<EntryType, BigDecimal[]>) totals;
  }

  private void sendTotals(CommandSender sender, Formats formats, String days,
      Map<EntryType, BigDecimal[]> totals) {
    BigDecimal fees = totals.values().stream().map(t -> t[1] == null ? BigDecimal.ZERO : t[1])
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    messages.send(sender, "status.money", Map.of(
        "<days>", days,
        "<spent>", formats.money(cash(totals, EntryType.BUY).negate()),
        "<received>", formats.money(cash(totals, EntryType.SELL)),
        "<fees>", formats.money(fees),
        "<dividends>", formats.money(cash(totals, EntryType.DIVIDEND))));
  }

  private static BigDecimal cash(Map<EntryType, BigDecimal[]> totals, EntryType type) {
    BigDecimal[] row = totals.get(type);
    return row == null || row[0] == null ? BigDecimal.ZERO : row[0];
  }

  private static String ago(Instant instant) {
    long minutes = Duration.between(instant, Instant.now()).toMinutes();
    return minutes < 60 ? minutes + "m ago" : minutes / 60 + "h ago";
  }

  /** Placeholders that describe an order in messages. */
  public static Map<String, String> orderValues(Order order, Formats formats) {
    return Map.of(
        "<id>", String.valueOf(order.id()),
        "<type>", order.type().name(),
        "<symbol>", order.symbol(),
        "<quantity>", formats.quantity(order.symbol(), order.shares()),
        "<target>", formats.money(order.target()),
        "<expires>", formats.date(order.expires()));
  }

  private Messages loadMessages() {
    return new Messages(YamlConfiguration.loadConfiguration(
        new File(getDataFolder(), "messages.yml")));
  }

  private Economy economy() {
    if (getServer().getPluginManager().getPlugin("Vault") == null) {
      return null;
    }
    RegisteredServiceProvider<Economy> provider =
        getServer().getServicesManager().getRegistration(Economy.class);
    return provider == null ? null : provider.getProvider();
  }

  private Database openDatabase(FileConfiguration config) throws SQLException {
    if ("mysql".equalsIgnoreCase(config.getString("storage.type"))) {
      return Database.mysql(config.getString("storage.mysql.host", "localhost"),
          config.getInt("storage.mysql.port", 3306),
          config.getString("storage.mysql.database", "stockmarket"),
          config.getString("storage.mysql.username", ""),
          config.getString("storage.mysql.password", ""));
    }
    return Database.sqlite(new File(getDataFolder(), "stockmarket.db"));
  }

  private static TradeRules rules(FileConfiguration config) {
    Set<String> blocked = new HashSet<>();
    config.getStringList("trading.blocked-exchanges").forEach(e -> blocked.add(e.toUpperCase()));
    return new TradeRules(
        decimal(config, "price-multiplier", "1"),
        decimal(config, "fees.flat", "0"),
        decimal(config, "fees.percent", "0"),
        config.getBoolean("fees.on-buy", true),
        config.getBoolean("fees.on-sell", true),
        decimal(config, "trading.minimum-price", "0"),
        Duration.ofMinutes(Math.max(1, config.getLong("trading.max-price-age-minutes", 15))),
        config.getBoolean("trading.allow-crypto", true),
        blocked,
        Math.max(1, config.getLong("trading.max-shares-per-trade", 10000)));
  }

  private static BigDecimal decimal(FileConfiguration config, String path, String fallback) {
    return new BigDecimal(config.getString(path, fallback));
  }

  private static java.util.concurrent.ThreadFactory threads(String name) {
    AtomicInteger count = new AtomicInteger();
    return task -> {
      Thread thread = new Thread(task, name + "-" + count.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    };
  }
}
