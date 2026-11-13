package com.maldahleh.stockmarket;

import com.maldahleh.stockmarket.actions.ActionService;
import com.maldahleh.stockmarket.actions.CorporateAction;
import com.maldahleh.stockmarket.orders.Order;
import com.maldahleh.stockmarket.orders.OrderBook;
import com.maldahleh.stockmarket.trade.TradeResult;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Player;
import com.maldahleh.stockmarket.bukkit.Brokers;
import com.maldahleh.stockmarket.bukkit.Formats;
import com.maldahleh.stockmarket.bukkit.Messages;
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
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.storage.Database;
import com.maldahleh.stockmarket.trade.TradeRules;
import com.maldahleh.stockmarket.trade.TradeService;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class StockMarketPlugin extends JavaPlugin {

  private static final String[] MENU_FILES = {"list.yml", "lookup.yml", "compare.yml",
      "portfolio.yml", "transactions.yml", "history.yml", "tutorial.yml", "messages.yml"};

  private ExecutorService network;
  private ExecutorService databaseThread;
  private Database database;
  private Messages messages;
  private Menus menus;
  private Services services;

  @Override
  public void onEnable() {
    saveFiles();
    messages = loadMessages();
    Economy economy = economy();
    if (economy == null) {
      disable("Vault and an economy plugin are required.", null);
      return;
    }

    FileConfiguration config = getConfig();
    registerCommand(config);
    String keyId = config.getString("alpaca.key-id", "").trim();
    String secret = config.getString("alpaca.secret", "").trim();
    if (keyId.isEmpty() || secret.isEmpty()) {
      getLogger().warning("No Alpaca key in config.yml, so trading is off. Add a free paper "
          + "trading key from https://alpaca.markets and restart.");
      return;
    }

    Portfolios portfolios = openStorage(config);
    if (portfolios != null) {
      start(config, economy, portfolios, new AlpacaProvider(keyId, secret,
          config.getString("alpaca.feed", "iex"), config.getBoolean("alpaca.live-account")));
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

  private void registerCommand(FileConfiguration config) {
    StockCommand command = new StockCommand(() -> services, messages,
        config.getBoolean("broker.disable-commands"), this::reloadFiles);
    var registered = getCommand("stockmarket");
    registered.setExecutor(command);
    registered.setTabCompleter(command);
  }

  /** Opens the database and loads holdings, or disables the plugin and returns null. */
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
      disable("Couldn't open the database or load holdings.", e);
      return null;
    }
  }

  private void disable(String reason, Exception error) {
    getLogger().log(Level.SEVERE, reason + " Disabling.", error);
    getServer().getPluginManager().disablePlugin(this);
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
        getLogger());
  }

  private void start(FileConfiguration config, Economy economy, Portfolios portfolios,
      MarketDataProvider provider) {
    PluginContext context = startThreads();
    MarketData market = new MarketData(provider, network,
        Duration.ofSeconds(Math.max(5, config.getLong("cache-seconds", 60))));
    TradeRules rules = rules(config);
    VaultWallet wallet = new VaultWallet(economy);
    Formats formats = new Formats(economy, Locale.forLanguageTag(config.getString("locale",
        "en-US")), config.getString("date-format", "yyyy-MM-dd HH:mm"),
        config.getString("unknown-data", "N/A"));

    TradeService trades = new TradeService(market, database, portfolios, wallet, rules, context);
    Trader trader = new Trader(trades, market, portfolios, messages, formats,
        decimal(config, "trading.confirm-above", "0"), context.serverThread());
    menus = new Menus(getDataFolder(), market, portfolios, database, trader, formats, messages,
        Math.max(1, config.getLong("trading.bulk-amount", 5)), context);
    Brokers brokers = new Brokers(this, config.getString("broker.name", "&8&lStock Broker"),
        menus);

    // The two services refer to each other, so each reaches the other through a reference.
    AtomicReference<OrderBook> orderBook = new AtomicReference<>();
    AtomicReference<ActionService> actionService = new AtomicReference<>();
    ActionService actions = new ActionService(market, database, portfolios, wallet, rules,
        config.getBoolean("dividends", true), actionNotifier(formats, orderBook),
        () -> orderBook.get().symbols(), context);
    actionService.set(actions);
    OrderBook orders = new OrderBook(market, database, portfolios, trades, rules,
        new OrderBook.Limits(Math.max(1, config.getInt("orders.max-open-per-player", 10)),
            Duration.ofDays(Math.max(1, config.getLong("orders.expire-days", 30)))),
        orderNotifier(formats, trader),
        (symbol, after, until) -> actionService.get().changedBetween(symbol, after, until),
        context);
    loadOrders(orders);
    orderBook.set(orders);
    services = new Services(menus, trader, brokers, portfolios, orders, formats);

    getServer().getPluginManager().registerEvents(new MenuListener(), this);
    getServer().getPluginManager().registerEvents(brokers, this);
    if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new StockExpansion(market, portfolios, rules, formats, getDescription().getVersion())
          .register();
    }
    schedule(config, actions, orders);
    checkConnection(market);
  }

  private ActionService.Notifier actionNotifier(Formats formats,
      AtomicReference<OrderBook> orderBook) {
    return new ActionService.Notifier() {
      @Override
      public void notify(UUID player, CorporateAction action, BigDecimal payout) {
        Player online = Bukkit.getPlayer(player);
        if (online == null) {
          return;
        }
        String paid = payout.signum() > 0 ? messages.raw("corporate-action-payout")
            .replace("<amount>", formats.money(payout)) : "";
        messages.send(online, "corporate-action", Map.of(
            "<action>", ActionService.describe(action), "<payout>", paid));
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

  private OrderBook.Notifier orderNotifier(Formats formats, Trader trader) {
    return new OrderBook.Notifier() {
      @Override
      public void filled(Order order, TradeResult.Success result) {
        Player online = Bukkit.getPlayer(order.owner());
        if (online != null) {
          messages.send(online, "orders.filled", orderValues(order, formats));
          trader.report(online, order.symbol(), result, order.type().buying());
        }
      }

      @Override
      public void cancelled(Order order, OrderBook.CancelCause cause) {
        Player online = Bukkit.getPlayer(order.owner());
        if (online != null) {
          Map<String, String> values = new HashMap<>(orderValues(order, formats));
          values.put("<reason>", messages.raw("orders.causes." + cause.name()));
          messages.send(online, "orders.cancelled", values);
        }
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

  private void schedule(FileConfiguration config, ActionService actions, OrderBook orders) {
    long period = Math.max(5, config.getLong("corporate-actions-check-minutes", 60)) * 60 * 20;
    Bukkit.getScheduler().runTaskTimer(this, actions::runOnce, 20L * 30, period);
    long orderPeriod = Math.max(15, config.getLong("orders.check-seconds", 60)) * 20;
    Bukkit.getScheduler().runTaskTimer(this, orders::check, 20L * 45, orderPeriod);
  }

  /** Checks the key now, so a typo shows up in the console rather than on a player's trade. */
  private void checkConnection(MarketData market) {
    market.clock().whenComplete((clock, error) -> {
      if (error != null) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        getLogger().severe("Market data check failed: " + cause.getMessage());
      } else {
        getLogger().info("Connected to market data. The US stock market is "
            + (clock.open() ? "open." : "closed."));
      }
    });
  }

  @Override
  public void onDisable() {
    if (network != null) {
      network.shutdownNow();
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
    }
    if (database != null) {
      database.close();
    }
  }

  /** Placeholders that describe an order in messages. */
  public static Map<String, String> orderValues(Order order, Formats formats) {
    return Map.of(
        "<id>", String.valueOf(order.id()),
        "<type>", order.type().name(),
        "<symbol>", order.symbol(),
        "<quantity>", formats.number(order.shares()),
        "<target>", formats.money(order.target()),
        "<expires>", formats.date(order.expires()));
  }

  private void reloadFiles() {
    messages.reload(YamlConfiguration.loadConfiguration(new File(getDataFolder(),
        "messages.yml")));
    if (menus != null) {
      menus.reload();
    }
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
        Math.max(1, config.getLong("trading.max-shares-per-trade", 10000)),
        Duration.ofSeconds(Math.max(0, config.getLong("trading.cooldown-seconds", 2))));
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
