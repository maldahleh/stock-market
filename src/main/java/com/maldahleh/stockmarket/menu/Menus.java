package com.maldahleh.stockmarket.menu;

import com.maldahleh.stockmarket.PluginContext;
import com.maldahleh.stockmarket.bukkit.Formats;
import com.maldahleh.stockmarket.bukkit.Messages;
import com.maldahleh.stockmarket.bukkit.Text;
import com.maldahleh.stockmarket.bukkit.Trader;
import com.maldahleh.stockmarket.market.Asset;
import com.maldahleh.stockmarket.market.AssetClass;
import com.maldahleh.stockmarket.market.DailyBar;
import com.maldahleh.stockmarket.market.MarketClock;
import com.maldahleh.stockmarket.market.MarketData;
import com.maldahleh.stockmarket.market.Quantities;
import com.maldahleh.stockmarket.market.Quote;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.LedgerEntry;
import com.maldahleh.stockmarket.portfolio.Portfolios;
import com.maldahleh.stockmarket.storage.Database;
import com.maldahleh.stockmarket.trade.TradeRules;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** Opens every menu. Data is fetched off the server thread; menus open on it. */
public class Menus {

  private static final int YEAR_OF_BARS = 252;
  private static final int LEDGER_LIMIT = 200;
  private static final int CONSOLE_LINES = 20;

  private final File folder;
  private final MarketData market;
  private final Portfolios portfolios;
  private final Database database;
  private final Trader trader;
  private final TradeRules rules;
  private final Formats formats;
  private final Messages messages;
  private final Executor serverThread;
  private final Executor databaseThread;
  private final Logger logger;
  private final Options options;

  private YamlConfiguration list;
  private YamlConfiguration lookup;
  private YamlConfiguration compare;
  private YamlConfiguration tutorial;
  private PagedTemplate portfolio;
  private PagedTemplate transactions;
  private PagedTemplate history;

  public Menus(File folder, MarketData market, Portfolios portfolios, Database database,
      Trader trader, Formats formats, Messages messages, Options options,
      PluginContext context) {
    this.folder = folder;
    this.market = market;
    this.portfolios = portfolios;
    this.database = database;
    this.trader = trader;
    this.rules = trader.rules();
    this.formats = formats;
    this.messages = messages;
    this.options = options;
    this.serverThread = context.serverThread();
    this.databaseThread = context.databaseThread();
    this.logger = context.logger();
    reload();
  }

  public void reload() {
    list = yaml("list.yml");
    lookup = yaml("lookup.yml");
    compare = yaml("compare.yml");
    tutorial = yaml("tutorial.yml");
    portfolio = PagedTemplate.load(yaml("portfolio.yml"));
    transactions = PagedTemplate.load(yaml("transactions.yml"));
    history = PagedTemplate.load(yaml("history.yml"));
  }

  private YamlConfiguration yaml(String name) {
    return YamlConfiguration.loadConfiguration(new File(folder, name));
  }

  // Static menus

  public void openTutorial(Player player) {
    Map<String, String> values = Map.of(
        "<multiplier>", rules.priceMultiplier().stripTrailingZeros().toPlainString(),
        "<broker-flat>", formats.money(rules.flatFee()),
        "<broker-percent>", rules.percentFee().stripTrailingZeros().toPlainString(),
        "<example>", formats.money(BigDecimal.valueOf(150).multiply(rules.priceMultiplier())
            .setScale(0, java.math.RoundingMode.HALF_UP)).replaceAll("[^0-9.,]", ""));
    MenuHolder menu = new MenuHolder(tutorial.getInt("size", 9),
        Text.color(tutorial.getString("name", "")));
    forEachItem(tutorial, (slot, item) -> menu.set(slot, item.build(values)));
    player.openInventory(menu.getInventory());
  }

  public void openList(Player player) {
    Map<Integer, ItemTemplate> items = new LinkedHashMap<>();
    forEachItem(list, items::put);
    List<String> symbols = items.values().stream().map(ItemTemplate::symbol)
        .filter(s -> s != null && !s.isBlank()).map(String::toUpperCase).distinct().toList();

    market.quotes(symbols).exceptionally(error -> {
      logger.warning("Couldn't load list prices: " + error.getMessage());
      return Map.of();
    }).thenCombine(clock(), ListData::new).thenAcceptAsync(data -> {
      if (!player.isOnline()) {
        return;
      }
      if (data.quotes().isEmpty() && !symbols.isEmpty()) {
        messages.send(player, "prices-unavailable");
      }
      String status = marketStatus("AAPL", data.clock());
      MenuHolder menu = new MenuHolder(list.getInt("size", 36),
          Text.render(list.getString("name", ""), Map.of("<market-status>", status)));
      items.forEach((slot, item) -> {
        if (item.symbol() == null || item.symbol().isBlank()) {
          menu.set(slot, item.build(Map.of()), click -> runAction(player, item.action()));
          return;
        }
        String symbol = item.symbol().toUpperCase();
        Map<String, String> values = StockView.placeholders(symbol,
            market.cachedAsset(symbol).orElse(null), data.quotes().get(symbol), null,
            owned(player, symbol), rules, formats);
        menu.set(slot, item.build(values),
            click -> openLookup(player, symbol, () -> openList(player)));
      });
      menu.onRefresh(() -> openList(player));
      player.openInventory(menu.getInventory());
    }, serverThread);
  }

  /** Symbols shown in the list, for tab completion. */
  public List<String> listSymbols() {
    List<String> symbols = new ArrayList<>();
    forEachItem(list, (slot, item) -> {
      if (item.symbol() != null && !item.symbol().isBlank()) {
        symbols.add(item.symbol().toUpperCase());
      }
    });
    return symbols;
  }

  private record ListData(Map<String, Quote> quotes, MarketClock clock) {}

  private CompletableFuture<MarketClock> clock() {
    return market.clock().exceptionally(error -> null);
  }

  // Stock menus

  public void openLookup(Player player, String rawSymbol) {
    openLookup(player, rawSymbol, null);
  }

  /** @param back reopens the menu the player came from; null hides the Back button */
  public void openLookup(Player player, String rawSymbol, Runnable back) {
    String symbol = rawSymbol.toUpperCase();
    load(symbol).thenAcceptAsync(loaded -> {
      if (!player.isOnline()) {
        return;
      }
      if (loaded.data().isEmpty()) {
        messages.send(player, loaded.unavailable() ? "failures.DATA_UNAVAILABLE"
            : "failures.INVALID_SYMBOL", Map.of("<symbol>", symbol));
        return;
      }
      showLookup(player, loaded.data().get(), back);
    }, serverThread);
  }

  private void showLookup(Player player, StockData stock, Runnable back) {
    String symbol = stock.symbol();
    Runnable reopen = () -> openLookup(player, symbol, back);
    Map<String, String> values = lookupValues(player, stock);
    MenuHolder menu = new MenuHolder(lookup.getInt("size", 27),
        Text.render(lookup.getString("name", ""), values));
    forEachItem(lookup, (slot, item) -> {
      if (item.trade()) {
        menu.set(slot, item.build(values), click -> tradeClick(player, symbol, click, reopen));
      } else if ("back".equals(item.action())) {
        if (back != null) {
          menu.set(slot, item.build(values), click -> back.run());
        }
      } else if ("refresh".equals(item.action())) {
        menu.set(slot, item.build(values), click -> reopen.run());
      } else {
        menu.set(slot, item.build(values), click -> runAction(player, item.action()));
      }
    });
    placeBars(menu, stock.bars());
    menu.onRefresh(reopen);
    player.openInventory(menu.getInventory());
  }

  private Map<String, String> lookupValues(Player player, StockData stock) {
    String symbol = stock.symbol();
    long owned = owned(player, symbol);
    Map<String, String> values = new HashMap<>(StockView.placeholders(symbol, stock.asset(),
        stock.quote(), stock.bars(), owned, rules, formats));
    long one = clickUnits(symbol);
    values.put("<one>", formats.quantity(symbol, one));
    values.put("<bulk>", formats.quantity(symbol, one * options.bulkAmount()));
    values.put("<market-status>", marketStatus(symbol, stock.clock()));
    values.put("<multiplier>", rules.priceMultiplier().stripTrailingZeros().toPlainString());
    BigDecimal price = stock.quote() == null ? null : rules.serverPrice(stock.quote().price());
    if (price == null) {
      values.put("<buy-one-total>", formats.unknown());
      values.put("<position-value>", formats.unknown());
      values.put("<position-net>", formats.unknown());
      return values;
    }
    BigDecimal oneValue = Quantities.value(symbol, price, one);
    values.put("<buy-one-total>", formats.money(oneValue.add(rules.fee(oneValue, true))));
    Holding holding = portfolios.get(player.getUniqueId(), symbol).orElse(null);
    BigDecimal worth = Quantities.value(symbol, price, owned);
    values.put("<position-value>", formats.money(worth));
    values.put("<position-net>", holding == null ? formats.unknown()
        : formats.tint(afterFees(worth).subtract(holding.costBasis()),
            formats.signedMoney(afterFees(worth).subtract(holding.costBasis()))));
    return values;
  }

  /** Daily bars, oldest to newest, green for up days and red for down days. */
  private void placeBars(MenuHolder menu, List<DailyBar> bars) {
    ConfigurationSection historical = lookup.getConfigurationSection("historical");
    if (historical == null) {
      return;
    }
    ItemTemplate up = ItemTemplate.load(historical.getConfigurationSection("up"));
    ItemTemplate down = ItemTemplate.load(historical.getConfigurationSection("down"));
    ItemTemplate none = ItemTemplate.load(historical.getConfigurationSection("no-data"));
    List<Integer> slots = historical.getIntegerList("slots");
    List<DailyBar> recent = bars.subList(Math.max(0, bars.size() - slots.size()), bars.size());
    if (recent.isEmpty() && !slots.isEmpty()) {
      menu.set(slots.get(0), none.build(Map.of()));
      return;
    }
    for (int i = 0; i < recent.size(); i++) {
      DailyBar bar = recent.get(i);
      boolean rose = bar.close() != null && bar.open() != null
          && bar.close().compareTo(bar.open()) >= 0;
      menu.set(slots.get(i), (rose ? up : down).build(StockView.bar(bar, formats)));
    }
  }

  public void openCompare(Player player, List<String> rawSymbols) {
    List<String> symbols = rawSymbols.stream().map(String::toUpperCase).distinct().toList();
    messages.send(player, "pending.compare");
    List<CompletableFuture<Loaded>> loads = symbols.stream().map(this::load).toList();
    CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new)).thenRunAsync(() -> {
      if (!player.isOnline()) {
        return;
      }
      List<Loaded> results = loads.stream().map(CompletableFuture::join).toList();
      reportMissing(player, results);
      List<StockData> found = results.stream().map(Loaded::data).flatMap(Optional::stream)
          .toList();
      if (!found.isEmpty()) {
        showCompare(player, symbols, found);
      }
    }, serverThread);
  }

  /** Names the symbols compare had to leave out, and why. */
  private void reportMissing(Player player, List<Loaded> results) {
    List<String> unknown = results.stream()
        .filter(r -> r.data().isEmpty() && !r.unavailable()).map(Loaded::symbol).toList();
    if (!unknown.isEmpty()) {
      messages.send(player, "compare-unknown", Map.of("<symbols>", String.join(", ", unknown)));
    }
    if (results.stream().anyMatch(Loaded::unavailable)) {
      messages.send(player, "failures.DATA_UNAVAILABLE");
    }
  }

  private void showCompare(Player player, List<String> symbols, List<StockData> found) {
    int perStock = compare.getInt("per-stock", 9);
    MenuHolder menu = new MenuHolder(perStock * found.size(),
        Text.color(compare.getString("name", "")));
    Runnable reopen = () -> openCompare(player, symbols);
    for (int i = 0; i < found.size(); i++) {
      StockData stock = found.get(i);
      Map<String, String> values = lookupValues(player, stock);
      int offset = i * perStock;
      forEachItem(compare, (slot, item) -> menu.set(offset + slot, item.build(values),
          click -> openLookup(player, stock.symbol(), reopen)));
    }
    menu.onRefresh(reopen);
    player.openInventory(menu.getInventory());
  }

  private record StockData(String symbol, Asset asset, Quote quote, List<DailyBar> bars,
      MarketClock clock) {}

  /**
   * What a stock lookup found.
   *
   * @param unavailable true if the data couldn't be loaded, as opposed to the symbol not existing
   */
  private record Loaded(String symbol, Optional<StockData> data, boolean unavailable) {}

  /** Everything a stock menu shows. */
  private CompletableFuture<Loaded> load(String symbol) {
    CompletableFuture<Optional<Asset>> asset = market.asset(symbol);
    CompletableFuture<Map<String, Quote>> quote = market.quotes(List.of(symbol))
        .exceptionally(e -> Map.of());
    CompletableFuture<List<DailyBar>> bars = market.dailyBars(symbol, YEAR_OF_BARS)
        .exceptionally(e -> List.of());
    CompletableFuture<MarketClock> marketClock = clock();
    return CompletableFuture.allOf(asset, quote, bars, marketClock).handle((ignored, error) -> {
      if (error != null) {
        logger.warning("Couldn't look up " + symbol + ": " + error.getMessage());
        return new Loaded(symbol, Optional.empty(), true);
      }
      return new Loaded(symbol, asset.join().map(a -> new StockData(symbol, a,
          quote.join().get(symbol), bars.join(), marketClock.join())), false);
    });
  }

  /** "Open", "Closed, opens in 3h 10m", or "Open 24/7" for crypto. */
  private String marketStatus(String symbol, MarketClock marketClock) {
    if (AssetClass.of(symbol) == AssetClass.CRYPTO) {
      return messages.raw("market-status.always");
    }
    if (marketClock == null) {
      return formats.unknown();
    }
    if (marketClock.open()) {
      return messages.raw("market-status.open");
    }
    return messages.raw("market-status.closed").replace("<reopens>",
        marketClock.nextOpen() == null ? messages.raw("reopens-soon")
            : formats.until(marketClock.nextOpen()));
  }

  // Paged menus

  public void openPortfolio(Player viewer, OfflinePlayer target) {
    List<Holding> holdings = portfolios.of(target.getUniqueId());
    List<String> symbols = holdings.stream().map(Holding::symbol).toList();
    Runnable reopen = () -> openPortfolio(viewer, target);
    market.quotes(symbols).exceptionally(e -> Map.of()).thenAcceptAsync(quotes -> {
      if (!viewer.isOnline()) {
        return;
      }
      String title = Text.render(portfolio.name(), Map.of("<player>", name(target)));
      openPaged(viewer, new Paged<>(portfolio, title, holdings,
          holding -> holdingValues(holding, quotes),
          (holding, back) -> openLookup(viewer, holding.symbol(), back),
          portfolioTotals(holdings, quotes), reopen), 0);
    }, serverThread);
  }

  /** What selling {@code worth} would actually pay, after the selling fee. */
  private BigDecimal afterFees(BigDecimal worth) {
    return worth.signum() == 0 ? worth : worth.subtract(rules.fee(worth, false));
  }

  private Map<String, String> holdingValues(Holding holding, Map<String, Quote> quotes) {
    Quote quote = quotes.get(holding.symbol());
    BigDecimal value = quote == null ? null
        : Quantities.value(holding.symbol(), rules.serverPrice(quote.price()), holding.shares());
    Map<String, String> values = new HashMap<>();
    values.put("<symbol>", holding.symbol());
    values.put("<name>", market.cachedAsset(holding.symbol()).map(Asset::name)
        .orElse(holding.symbol()));
    values.put("<quantity>", formats.quantity(holding.symbol(), holding.shares()));
    values.put("<purchase-value>", formats.money(holding.costBasis()));
    values.put("<average-cost>", formats.money(holding.averageCost()));
    values.put("<current-value>", formats.money(value));
    values.put("<sell-value>", value == null ? formats.unknown() : formats.money(afterFees(value)));
    BigDecimal net = value == null ? null : afterFees(value).subtract(holding.costBasis());
    values.put("<net>", formats.tint(net, formats.signedMoney(net)));
    BigDecimal change = quote == null ? null : quote.changeFromClose();
    values.put("<change-close>", formats.tint(change, formats.percent(change)));
    return values;
  }

  /** What the whole portfolio cost and would pay if sold now; unknown if a price is missing. */
  private Map<String, String> portfolioTotals(List<Holding> holdings, Map<String, Quote> quotes) {
    BigDecimal paid = holdings.stream().map(Holding::costBasis)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal worth = BigDecimal.ZERO;
    BigDecimal sellValue = BigDecimal.ZERO;
    for (Holding holding : holdings) {
      Quote quote = quotes.get(holding.symbol());
      if (quote == null) {
        return Map.of("<purchase-value>", formats.money(paid), "<current-value>",
            formats.unknown(), "<sell-value>", formats.unknown(), "<net-value>",
            formats.unknown(), "<net-percent>", formats.unknown());
      }
      BigDecimal value = Quantities.value(holding.symbol(), rules.serverPrice(quote.price()),
          holding.shares());
      worth = worth.add(value);
      sellValue = sellValue.add(afterFees(value));
    }
    BigDecimal net = sellValue.subtract(paid);
    BigDecimal percent = StockView.change(sellValue, paid);
    return Map.of(
        "<purchase-value>", formats.money(paid),
        "<current-value>", formats.money(worth),
        "<sell-value>", formats.money(sellValue),
        "<net-value>", formats.tint(net, formats.signedMoney(net)),
        "<net-percent>", formats.tint(percent, formats.percent(percent)));
  }

  public void openTransactions(Player viewer, OfflinePlayer target) {
    Runnable reopen = () -> openTransactions(viewer, target);
    ledger(target.getUniqueId(), null).thenAcceptAsync(entries -> openLedger(viewer,
        transactions, Text.render(transactions.name(), Map.of("<player>", name(target))),
        entries, reopen), serverThread);
  }

  public void openHistory(Player viewer, String symbol) {
    String upper = symbol == null ? null : symbol.toUpperCase();
    Runnable reopen = () -> openHistory(viewer, symbol);
    ledger(null, upper).thenAcceptAsync(entries -> openLedger(viewer, history,
        Text.render(history.name(), Map.of("<symbol>", upper == null ? "" : upper)), entries,
        reopen), serverThread);
  }

  private CompletableFuture<List<LedgerEntry>> ledger(UUID owner, String symbol) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return database.ledger(owner, symbol, LEDGER_LIMIT);
      } catch (SQLException e) {
        throw new CompletionException(e);
      }
    }, databaseThread).exceptionally(error -> {
      logger.log(Level.WARNING, "Couldn't read the ledger", error);
      return List.of();
    });
  }

  private void openLedger(Player viewer, PagedTemplate template, String title,
      List<LedgerEntry> entries, Runnable reopen) {
    if (!viewer.isOnline()) {
      return;
    }
    openPaged(viewer, new Paged<>(template, title, entries, this::entryValues,
        (entry, back) -> openLookup(viewer, entry.symbol(), back), Map.of(), reopen), 0);
  }

  private Map<String, String> entryValues(LedgerEntry entry) {
    Map<String, String> values = new HashMap<>();
    values.put("<symbol>", entry.symbol());
    values.put("<date>", formats.date(entry.time()));
    values.put("<name>", name(Bukkit.getOfflinePlayer(entry.owner())));
    values.put("<transaction-type>", messages.raw("entry-types." + entry.type().name()));
    values.put("<quantity>", (entry.shares() > 0 ? "+" : "")
        + formats.quantity(entry.symbol(), entry.shares()));
    values.put("<price>", formats.money(entry.price()));
    values.put("<broker-fees>", formats.money(entry.fee()));
    values.put("<cash>", formats.tint(entry.cash(), formats.signedMoney(entry.cash())));
    return values;
  }

  /**
   * One paged menu's contents.
   *
   * @param onClick opens an item's details, given a way back to this page
   * @param extras placeholders for the template's extra items
   * @param refresh rebuilds the menu from fresh data
   */
  private record Paged<T>(PagedTemplate template, String title, List<T> items,
      Function<T, Map<String, String>> placeholders, BiConsumer<T, Runnable> onClick,
      Map<String, String> extras, Runnable refresh) {}

  private <T> void openPaged(Player viewer, Paged<T> paged, int page) {
    PagedTemplate template = paged.template();
    List<Integer> slots = template.contentSlots();
    int perPage = Math.max(1, slots.size());
    int pages = Math.max(1, (paged.items().size() + perPage - 1) / perPage);
    int current = Math.max(0, Math.min(page, pages - 1));
    Map<String, String> pageValues = Map.of("<page>", String.valueOf(current + 1),
        "<pages>", String.valueOf(pages));

    MenuHolder menu = new MenuHolder(template.size(), paged.title());
    template.extraItems().forEach((slot, item) -> menu.set(slot, item.build(paged.extras())));
    Runnable thisPage = () -> openPaged(viewer, paged, current);
    int start = current * perPage;
    for (int i = 0; i < perPage && start + i < paged.items().size(); i++) {
      T item = paged.items().get(start + i);
      menu.set(slots.get(i), template.baseItem().build(paged.placeholders().apply(item)),
          click -> paged.onClick().accept(item, thisPage));
    }
    if (paged.items().isEmpty() && !slots.isEmpty() && template.empty() != null) {
      menu.set(slots.get(0), template.empty().build(Map.of()));
    }
    menu.set(template.previousSlot(), (current > 0 ? template.previous()
        : template.noPrevious()).build(pageValues),
        click -> openPaged(viewer, paged, current - 1));
    menu.set(template.nextSlot(), (current < pages - 1 ? template.next()
        : template.noNext()).build(pageValues),
        click -> openPaged(viewer, paged, current + 1));
    menu.onRefresh(paged.refresh());
    viewer.openInventory(menu.getInventory());
  }

  // Text reports, for the console and moderators

  /** The target's holdings as chat lines. */
  public void printPortfolio(CommandSender viewer, OfflinePlayer target) {
    List<Holding> holdings = portfolios.of(target.getUniqueId());
    market.quotes(holdings.stream().map(Holding::symbol).toList()).exceptionally(e -> Map.of())
        .thenAcceptAsync(quotes -> {
          Map<String, String> totals = portfolioTotals(holdings, quotes);
          Map<String, String> header = new HashMap<>(totals);
          header.put("<player>", name(target));
          messages.send(viewer, "console.portfolio-header", header);
          for (Holding holding : holdings) {
            messages.sendBlock(viewer, "console.portfolio-line", holdingValues(holding, quotes));
          }
        }, serverThread);
  }

  /** The target's most recent ledger entries as chat lines. */
  public void printTransactions(CommandSender viewer, OfflinePlayer target) {
    ledger(target.getUniqueId(), null).thenAcceptAsync(entries -> {
      messages.send(viewer, "console.transactions-header", Map.of("<player>", name(target)));
      entries.stream().limit(CONSOLE_LINES)
          .forEach(entry -> messages.sendBlock(viewer, "console.transaction-line",
              entryValues(entry)));
    }, serverThread);
  }

  // Helpers

  private void tradeClick(Player player, String symbol, ClickType click, Runnable reopen) {
    long one = clickUnits(symbol);
    switch (click) {
      case LEFT -> trader.buy(player, symbol, one, reopen);
      case SHIFT_LEFT -> trader.buy(player, symbol, one * options.bulkAmount(), reopen);
      case RIGHT -> trader.sell(player, symbol, one, reopen);
      case SHIFT_RIGHT -> trader.sellAll(player, symbol, reopen);
      default -> { }
    }
  }

  /** One click's worth: a share, or {@code crypto-click-amount} of a coin. */
  private long clickUnits(String symbol) {
    return AssetClass.of(symbol) == AssetClass.CRYPTO
        ? Quantities.of(symbol, options.cryptoClickAmount()) : 1;
  }

  /** What a navigation item does. */
  private void runAction(Player player, String action) {
    if (action == null || action.isBlank()) {
      return;
    }
    if (!action.equals("close") && !player.hasPermission("stockmarket." + action)) {
      messages.send(player, "no-permission");
      return;
    }
    switch (action) {
      case "portfolio" -> openPortfolio(player, player);
      case "transactions" -> openTransactions(player, player);
      case "history" -> openHistory(player, null);
      case "tutorial" -> openTutorial(player);
      case "orders" -> {
        player.closeInventory();
        options.showOrders().accept(player);
      }
      case "close" -> player.closeInventory();
      default -> { }
    }
  }

  /**
   * Menu settings from config.yml.
   *
   * @param bulkAmount clicks with shift trade this many times the normal amount
   * @param cryptoClickAmount how much of a coin one click trades
   * @param showOrders lists a player's open orders in chat
   */
  public record Options(long bulkAmount, BigDecimal cryptoClickAmount,
      Consumer<Player> showOrders) {}

  private long owned(Player player, String symbol) {
    return portfolios.get(player.getUniqueId(), symbol).map(Holding::shares).orElse(0L);
  }

  private static String name(OfflinePlayer player) {
    return player.getName() == null ? player.getUniqueId().toString().substring(0, 8)
        : player.getName();
  }

  private static void forEachItem(ConfigurationSection yaml,
      BiConsumer<Integer, ItemTemplate> consumer) {
    ConfigurationSection items = yaml.getConfigurationSection("items");
    if (items == null) {
      return;
    }
    for (String key : items.getKeys(false)) {
      try {
        consumer.accept(Integer.parseInt(key), ItemTemplate.load(items.getConfigurationSection(key)));
      } catch (NumberFormatException ignored) {
        // Only numbered slots are items.
      }
    }
  }
}
