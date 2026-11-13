package com.maldahleh.stockmarket.menu;

import com.maldahleh.stockmarket.bukkit.Formats;
import com.maldahleh.stockmarket.bukkit.Messages;
import com.maldahleh.stockmarket.bukkit.Text;
import com.maldahleh.stockmarket.bukkit.Trader;
import com.maldahleh.stockmarket.market.Asset;
import com.maldahleh.stockmarket.market.DailyBar;
import com.maldahleh.stockmarket.market.MarketData;
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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** Opens every menu. Data is fetched off the server thread; menus open on it. */
public class Menus {

  private static final int YEAR_OF_BARS = 252;
  private static final int LEDGER_LIMIT = 200;

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
  private final long bulkAmount;

  private YamlConfiguration list;
  private YamlConfiguration lookup;
  private YamlConfiguration compare;
  private YamlConfiguration tutorial;
  private PagedTemplate portfolio;
  private PagedTemplate transactions;
  private PagedTemplate history;

  public Menus(File folder, MarketData market, Portfolios portfolios, Database database,
      Trader trader, TradeRules rules, Formats formats, Messages messages, Executor serverThread,
      Executor databaseThread, Logger logger, long bulkAmount) {
    this.bulkAmount = bulkAmount;
    this.folder = folder;
    this.market = market;
    this.portfolios = portfolios;
    this.database = database;
    this.trader = trader;
    this.rules = rules;
    this.formats = formats;
    this.messages = messages;
    this.serverThread = serverThread;
    this.databaseThread = databaseThread;
    this.logger = logger;
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
    MenuHolder menu = new MenuHolder(tutorial.getInt("size", 9),
        Text.color(tutorial.getString("name", "")));
    forEachItem(tutorial, (slot, item) -> menu.set(slot, item.build(Map.of())));
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
    }).thenAcceptAsync(quotes -> {
      if (!player.isOnline()) {
        return;
      }
      MenuHolder menu = new MenuHolder(list.getInt("size", 36),
          Text.color(list.getString("name", "")));
      items.forEach((slot, item) -> {
        if (item.symbol() == null || item.symbol().isBlank()) {
          menu.set(slot, item.build(Map.of()));
          return;
        }
        String symbol = item.symbol().toUpperCase();
        Map<String, String> values = new HashMap<>(StockView.placeholders(symbol,
            market.cachedAsset(symbol).orElse(null), quotes.get(symbol), null,
            owned(player, symbol), rules, formats));
        values.put("<bulk>", String.valueOf(bulkAmount));
        menu.set(slot, item.build(values), click -> tradeClick(player, symbol, click));
      });
      player.openInventory(menu.getInventory());
    }, serverThread);
  }

  // Stock menus

  public void openLookup(Player player, String rawSymbol) {
    String symbol = rawSymbol.toUpperCase();
    messages.send(player, "pending.lookup", Map.of("<symbol>", symbol));
    load(symbol).thenAcceptAsync(data -> {
      if (!player.isOnline()) {
        return;
      }
      if (data.isEmpty()) {
        messages.send(player, "failures.INVALID_SYMBOL", Map.of("<symbol>", symbol));
        return;
      }
      StockData stock = data.get();
      Map<String, String> values = new HashMap<>(StockView.placeholders(symbol, stock.asset(),
          stock.quote(), stock.bars(), owned(player, symbol), rules, formats));
      values.put("<bulk>", String.valueOf(bulkAmount));
      MenuHolder menu = new MenuHolder(lookup.getInt("size", 36),
          Text.render(lookup.getString("name", ""), values));
      forEachItem(lookup, (slot, item) -> {
        if (item.trade()) {
          menu.set(slot, item.build(values), click -> tradeClick(player, symbol, click));
        } else {
          menu.set(slot, item.build(values));
        }
      });

      ConfigurationSection historical = lookup.getConfigurationSection("historical");
      if (historical != null) {
        ItemTemplate data1 = ItemTemplate.load(historical.getConfigurationSection("data"));
        ItemTemplate none = ItemTemplate.load(historical.getConfigurationSection("no-data"));
        List<Integer> slots = historical.getIntegerList("slots");
        List<DailyBar> newestFirst = new ArrayList<>(stock.bars());
        java.util.Collections.reverse(newestFirst);
        for (int i = 0; i < slots.size(); i++) {
          menu.set(slots.get(i), i < newestFirst.size()
              ? data1.build(StockView.bar(newestFirst.get(i), formats)) : none.build(Map.of()));
        }
      }
      player.openInventory(menu.getInventory());
    }, serverThread);
  }

  public void openCompare(Player player, List<String> rawSymbols) {
    List<String> symbols = rawSymbols.stream().map(String::toUpperCase).distinct().toList();
    messages.send(player, "pending.compare");
    List<CompletableFuture<Optional<StockData>>> loads = symbols.stream().map(this::load).toList();
    CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new)).thenRunAsync(() -> {
      if (!player.isOnline()) {
        return;
      }
      int perStock = compare.getInt("per-stock", 18);
      List<StockData> found = loads.stream().map(CompletableFuture::join)
          .flatMap(Optional::stream).toList();
      if (found.isEmpty()) {
        messages.send(player, "no-data");
        return;
      }
      MenuHolder menu = new MenuHolder(perStock * found.size(),
          Text.color(compare.getString("name", "")));
      for (int i = 0; i < found.size(); i++) {
        StockData stock = found.get(i);
        Map<String, String> values = StockView.placeholders(stock.symbol(), stock.asset(),
            stock.quote(), stock.bars(), owned(player, stock.symbol()), rules, formats);
        int offset = i * perStock;
        forEachItem(compare, (slot, item) -> menu.set(offset + slot, item.build(values),
            click -> openLookup(player, stock.symbol())));
      }
      player.openInventory(menu.getInventory());
    }, serverThread);
  }

  private record StockData(String symbol, Asset asset, Quote quote, List<DailyBar> bars) {}

  /** Everything a stock menu shows. Empty if the symbol isn't listed. */
  private CompletableFuture<Optional<StockData>> load(String symbol) {
    CompletableFuture<Optional<Asset>> asset = market.asset(symbol);
    CompletableFuture<Map<String, Quote>> quote = market.quotes(List.of(symbol))
        .exceptionally(e -> Map.of());
    CompletableFuture<List<DailyBar>> bars = market.dailyBars(symbol, YEAR_OF_BARS)
        .exceptionally(e -> List.of());
    return CompletableFuture.allOf(asset, quote, bars).handle((ignored, error) -> {
      if (error != null) {
        logger.warning("Couldn't look up " + symbol + ": " + error.getMessage());
        return Optional.empty();
      }
      return asset.join().map(a -> new StockData(symbol, a, quote.join().get(symbol),
          bars.join()));
    });
  }

  // Paged menus

  public void openPortfolio(Player viewer, OfflinePlayer target) {
    List<Holding> holdings = portfolios.of(target.getUniqueId());
    List<String> symbols = holdings.stream().map(Holding::symbol).toList();
    market.quotes(symbols).exceptionally(e -> Map.of()).thenAcceptAsync(quotes -> {
      if (!viewer.isOnline()) {
        return;
      }
      String title = Text.render(portfolio.name(), Map.of("<player>", name(target)));
      BigDecimal paid = BigDecimal.ZERO;
      BigDecimal worth = BigDecimal.ZERO;
      boolean complete = true;
      for (Holding holding : holdings) {
        Quote quote = quotes.get(holding.symbol());
        paid = paid.add(holding.costBasis());
        if (quote == null) {
          complete = false;
        } else {
          worth = worth.add(rules.serverPrice(quote.price())
              .multiply(BigDecimal.valueOf(holding.shares())));
        }
      }
      Map<String, String> totals = Map.of(
          "<purchase-value>", formats.money(paid),
          "<current-value>", complete ? formats.money(worth) : formats.unknown(),
          "<net-value>", complete ? formats.signedMoney(worth.subtract(paid)) : formats.unknown(),
          "<net-percent>", complete ? formats.percent(StockView.change(worth, paid))
              : formats.unknown());
      openPaged(viewer, portfolio, title, holdings, holding -> {
        Quote quote = quotes.get(holding.symbol());
        BigDecimal value = quote == null ? null
            : rules.serverPrice(quote.price()).multiply(BigDecimal.valueOf(holding.shares()));
        Map<String, String> values = new HashMap<>();
        values.put("<symbol>", holding.symbol());
        values.put("<name>", market.cachedAsset(holding.symbol()).map(Asset::name)
            .orElse(holding.symbol()));
        values.put("<quantity>", formats.number(holding.shares()));
        values.put("<purchase-value>", formats.money(holding.costBasis()));
        values.put("<average-cost>", formats.money(holding.averageCost()));
        values.put("<current-value>", formats.money(value));
        values.put("<net>", value == null ? formats.unknown()
            : formats.signedMoney(value.subtract(holding.costBasis())));
        values.put("<change-close>", formats.percent(quote == null ? null
            : quote.changeFromClose()));
        return values;
      }, (holding, click) -> openLookup(viewer, holding.symbol()), totals, 0);
    }, serverThread);
  }

  public void openTransactions(Player viewer, OfflinePlayer target) {
    ledger(target.getUniqueId(), null).thenAcceptAsync(entries -> openLedger(viewer,
        transactions, Text.render(transactions.name(), Map.of("<player>", name(target))),
        entries), serverThread);
  }

  public void openHistory(Player viewer, String symbol) {
    String upper = symbol == null ? null : symbol.toUpperCase();
    ledger(null, upper).thenAcceptAsync(entries -> openLedger(viewer, history,
        Text.render(history.name(), Map.of("<symbol>", upper == null ? "" : upper)), entries),
        serverThread);
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
      List<LedgerEntry> entries) {
    if (!viewer.isOnline()) {
      return;
    }
    openPaged(viewer, template, title, entries, entry -> {
      Map<String, String> values = new HashMap<>();
      values.put("<symbol>", entry.symbol());
      values.put("<date>", formats.date(entry.time()));
      values.put("<name>", name(Bukkit.getOfflinePlayer(entry.owner())));
      values.put("<transaction-type>", messages.raw("entry-types." + entry.type().name()));
      values.put("<quantity>", (entry.shares() > 0 ? "+" : "") + formats.number(entry.shares()));
      values.put("<price>", formats.money(entry.price()));
      values.put("<broker-fees>", formats.money(entry.fee()));
      values.put("<cash>", formats.signedMoney(entry.cash()));
      return values;
    }, (entry, click) -> openLookup(viewer, entry.symbol()), Map.of(), 0);
  }

  private <T> void openPaged(Player viewer, PagedTemplate template, String title, List<T> items,
      Function<T, Map<String, String>> placeholders, BiConsumer<T, ClickType> onClick,
      Map<String, String> extras, int page) {
    List<Integer> slots = template.contentSlots();
    int perPage = Math.max(1, slots.size());
    int pages = Math.max(1, (items.size() + perPage - 1) / perPage);
    int current = Math.max(0, Math.min(page, pages - 1));

    MenuHolder menu = new MenuHolder(template.size(), title);
    template.extraItems().forEach((slot, item) -> menu.set(slot, item.build(extras)));
    int start = current * perPage;
    for (int i = 0; i < perPage && start + i < items.size(); i++) {
      T item = items.get(start + i);
      menu.set(slots.get(i), template.baseItem().build(placeholders.apply(item)),
          click -> onClick.accept(item, click));
    }
    if (items.isEmpty() && !slots.isEmpty()) {
      menu.set(slots.get(0), new ItemTemplate(org.bukkit.Material.BARRIER,
          messages.raw("no-data"), List.of(), null, false).build(Map.of()));
    }

    if (current > 0) {
      menu.set(template.previousSlot(), template.previous().build(Map.of()),
          click -> openPaged(viewer, template, title, items, placeholders, onClick, extras, current - 1));
    } else {
      menu.set(template.previousSlot(), template.noPrevious().build(Map.of()));
    }
    if (current < pages - 1) {
      menu.set(template.nextSlot(), template.next().build(Map.of()),
          click -> openPaged(viewer, template, title, items, placeholders, onClick, extras, current + 1));
    } else {
      menu.set(template.nextSlot(), template.noNext().build(Map.of()));
    }
    viewer.openInventory(menu.getInventory());
  }

  // Helpers

  private void tradeClick(Player player, String symbol, ClickType click) {
    switch (click) {
      case LEFT -> trader.buy(player, symbol, 1, true);
      case SHIFT_LEFT -> trader.buy(player, symbol, bulkAmount, true);
      case RIGHT -> trader.sell(player, symbol, 1, true);
      case SHIFT_RIGHT -> trader.sellAll(player, symbol, true);
      case MIDDLE, DROP -> openLookup(player, symbol);
      default -> { }
    }
  }

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
