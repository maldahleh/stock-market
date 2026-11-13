package com.maldahleh.stockmarket.storage;

import com.maldahleh.stockmarket.actions.ActionApplier.Outcome;
import com.maldahleh.stockmarket.orders.Order;
import com.maldahleh.stockmarket.orders.OrderType;
import com.maldahleh.stockmarket.portfolio.EntryType;
import com.maldahleh.stockmarket.portfolio.Holding;
import com.maldahleh.stockmarket.portfolio.LedgerEntry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Holdings, the ledger and applied corporate actions. Every write that moves shares also writes
 * its ledger entries in the same database transaction. Calls block; run them off the server
 * thread.
 */
public class Database implements AutoCloseable {

  private final HikariDataSource source;
  private final boolean mysql;

  private Database(HikariDataSource source, boolean mysql) {
    this.source = source;
    this.mysql = mysql;
  }

  public static Database sqlite(File file) throws SQLException {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
    config.setDriverClassName("org.sqlite.JDBC");
    // SQLite allows one writer; a single connection keeps writes in order.
    config.setMaximumPoolSize(1);
    config.setPoolName("StockMarket-SQLite");
    return open(config, false);
  }

  public static Database mysql(String host, int port, String database, String user,
      String password) throws SQLException {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
    config.setUsername(user);
    config.setPassword(password);
    config.setMaximumPoolSize(4);
    config.setPoolName("StockMarket-MySQL");
    return open(config, true);
  }

  private static Database open(HikariConfig config, boolean mysql) throws SQLException {
    Database database = new Database(new HikariDataSource(config), mysql);
    database.createTables();
    return database;
  }

  private void createTables() throws SQLException {
    String id = mysql ? "BIGINT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
    try (Connection c = source.getConnection(); Statement s = c.createStatement()) {
      s.executeUpdate("CREATE TABLE IF NOT EXISTS sm_holdings (owner CHAR(36) NOT NULL, "
          + "symbol VARCHAR(32) NOT NULL, shares BIGINT NOT NULL, "
          + "cost_basis DECIMAL(24,4) NOT NULL, PRIMARY KEY (owner, symbol))");
      s.executeUpdate("CREATE TABLE IF NOT EXISTS sm_ledger (id " + id + ", "
          + "owner CHAR(36) NOT NULL, type VARCHAR(16) NOT NULL, symbol VARCHAR(32) NOT NULL, "
          + "shares BIGINT NOT NULL, price DECIMAL(24,4), fee DECIMAL(24,4) NOT NULL, "
          + "cash DECIMAL(24,4) NOT NULL, created_at BIGINT NOT NULL, reference VARCHAR(64))");
      s.executeUpdate("CREATE TABLE IF NOT EXISTS sm_applied_actions (id VARCHAR(64) NOT NULL "
          + "PRIMARY KEY, applied_at BIGINT NOT NULL)");
      s.executeUpdate("CREATE TABLE IF NOT EXISTS sm_orders (id " + id + ", "
          + "owner CHAR(36) NOT NULL, symbol VARCHAR(32) NOT NULL, type VARCHAR(16) NOT NULL, "
          + "shares BIGINT NOT NULL, target DECIMAL(24,4) NOT NULL, created_at BIGINT NOT NULL, "
          + "expires_at BIGINT NOT NULL)");
      s.executeUpdate("CREATE TABLE IF NOT EXISTS sm_meta (name VARCHAR(64) NOT NULL PRIMARY KEY, "
          + "value VARCHAR(255) NOT NULL)");
      createIndex(s, "sm_ledger_owner", "sm_ledger (owner, created_at)");
      createIndex(s, "sm_ledger_symbol", "sm_ledger (symbol, created_at)");
    }
  }

  private void createIndex(Statement s, String name, String target) throws SQLException {
    if (!mysql) {
      s.executeUpdate("CREATE INDEX IF NOT EXISTS " + name + " ON " + target);
      return;
    }
    try {
      s.executeUpdate("CREATE INDEX " + name + " ON " + target);
    } catch (SQLException e) {
      // MySQL has no IF NOT EXISTS for indexes; 1061 means it already exists.
      if (e.getErrorCode() != 1061) {
        throw e;
      }
    }
  }

  public Map<UUID, Map<String, Holding>> loadHoldings() throws SQLException {
    Map<UUID, Map<String, Holding>> holdings = new HashMap<>();
    try (Connection c = source.getConnection();
        PreparedStatement ps = c.prepareStatement(
            "SELECT owner, symbol, shares, cost_basis FROM sm_holdings WHERE shares > 0");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        Holding holding = new Holding(UUID.fromString(rs.getString(1)), rs.getString(2),
            rs.getLong(3), rs.getBigDecimal(4));
        holdings.computeIfAbsent(holding.owner(), k -> new HashMap<>())
            .put(holding.symbol(), holding);
      }
    }
    return holdings;
  }

  /** Saves new holding states and their ledger entries together. */
  public void save(Collection<Holding> holdings, Collection<LedgerEntry> entries)
      throws SQLException {
    try (Connection c = source.getConnection()) {
      c.setAutoCommit(false);
      try {
        writeHoldings(c, holdings);
        writeEntries(c, entries);
        c.commit();
      } catch (SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }

  /**
   * Saves a corporate action's changes and marks it applied, all or nothing.
   *
   * @return false if the action was already applied, in which case nothing is written
   */
  public boolean applyAction(String actionId, Outcome outcome) throws SQLException {
    try (Connection c = source.getConnection()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement check = c.prepareStatement(
            "SELECT 1 FROM sm_applied_actions WHERE id = ?")) {
          check.setString(1, actionId);
          try (ResultSet rs = check.executeQuery()) {
            if (rs.next()) {
              c.rollback();
              return false;
            }
          }
        }
        try (PreparedStatement mark = c.prepareStatement(
            "INSERT INTO sm_applied_actions (id, applied_at) VALUES (?, ?)")) {
          mark.setString(1, actionId);
          mark.setLong(2, System.currentTimeMillis());
          mark.executeUpdate();
        }
        writeHoldings(c, outcome.holdings());
        writeEntries(c, outcome.entries());
        c.commit();
        return true;
      } catch (SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }

  /** Thrown by {@link #sell} when the owner doesn't have enough shares. */
  public static class InsufficientSharesException extends Exception {
    public InsufficientSharesException() {
      super("Not enough shares");
    }
  }

  /** @param basisRemoved the part of the cost basis that left with the sold shares */
  public record Sale(Holding holding, BigDecimal basisRemoved) {}

  /** Adds bought shares and their cost, and records the entry. Returns the new holding. */
  public Holding buy(UUID owner, String symbol, long shares, BigDecimal cost, LedgerEntry entry)
      throws SQLException {
    try (Connection c = source.getConnection()) {
      c.setAutoCommit(false);
      try {
        Holding current = readHolding(c, owner, symbol);
        Holding updated = new Holding(owner, symbol, current.shares() + shares,
            current.costBasis().add(cost));
        writeHoldings(c, List.of(updated));
        writeEntries(c, List.of(entry));
        c.commit();
        return updated;
      } catch (SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }

  /**
   * Removes sold shares and their share of the cost basis, and records the entry. Checks the
   * stored holding, so two sales can never sell the same shares.
   */
  public Sale sell(UUID owner, String symbol, long shares, LedgerEntry entry)
      throws SQLException, InsufficientSharesException {
    try (Connection c = source.getConnection()) {
      c.setAutoCommit(false);
      try {
        Holding current = readHolding(c, owner, symbol);
        if (current.shares() < shares) {
          c.rollback();
          throw new InsufficientSharesException();
        }
        BigDecimal removed = current.costBasis().multiply(BigDecimal.valueOf(shares))
            .divide(BigDecimal.valueOf(current.shares()), 4, java.math.RoundingMode.HALF_UP);
        long left = current.shares() - shares;
        Holding updated = new Holding(owner, symbol, left,
            left == 0 ? BigDecimal.ZERO : current.costBasis().subtract(removed));
        writeHoldings(c, List.of(updated));
        writeEntries(c, List.of(entry));
        c.commit();
        return new Sale(updated, removed);
      } catch (SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }

  /** Current holdings of the given symbols, by owner. */
  public List<Holding> holdersOf(String symbol) throws SQLException {
    List<Holding> holdings = new ArrayList<>();
    try (Connection c = source.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT owner, symbol, shares, cost_basis "
            + "FROM sm_holdings WHERE symbol = ? AND shares > 0")) {
      ps.setString(1, symbol);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          holdings.add(new Holding(UUID.fromString(rs.getString(1)), rs.getString(2),
              rs.getLong(3), rs.getBigDecimal(4)));
        }
      }
    }
    return holdings;
  }

  /** Owners who sold all their shares at or after {@code since}; they may still be owed. */
  public List<UUID> recentSellers(String symbol, Instant since) throws SQLException {
    List<UUID> owners = new ArrayList<>();
    try (Connection c = source.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT DISTINCT owner FROM sm_ledger "
            + "WHERE symbol = ? AND created_at >= ? AND shares < 0")) {
      ps.setString(1, symbol);
      ps.setLong(2, since.toEpochMilli());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          owners.add(UUID.fromString(rs.getString(1)));
        }
      }
    }
    return owners;
  }

  public Map<String, Holding> holdingsOf(UUID owner) throws SQLException {
    Map<String, Holding> holdings = new HashMap<>();
    try (Connection c = source.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT owner, symbol, shares, cost_basis "
            + "FROM sm_holdings WHERE owner = ? AND shares > 0")) {
      ps.setString(1, owner.toString());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          holdings.put(rs.getString(2), new Holding(owner, rs.getString(2), rs.getLong(3),
              rs.getBigDecimal(4)));
        }
      }
    }
    return holdings;
  }

  private static Holding readHolding(Connection c, UUID owner, String symbol)
      throws SQLException {
    try (PreparedStatement ps = c.prepareStatement(
        "SELECT shares, cost_basis FROM sm_holdings WHERE owner = ? AND symbol = ?")) {
      ps.setString(1, owner.toString());
      ps.setString(2, symbol);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? new Holding(owner, symbol, rs.getLong(1), rs.getBigDecimal(2))
            : new Holding(owner, symbol, 0, BigDecimal.ZERO);
      }
    }
  }

  public boolean isApplied(String actionId) throws SQLException {
    try (Connection c = source.getConnection();
        PreparedStatement ps = c.prepareStatement(
            "SELECT 1 FROM sm_applied_actions WHERE id = ?")) {
      ps.setString(1, actionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  /** Net shares of {@code symbol} the owner gained at or after {@code since}, from the ledger. */
  public long sharesChangedSince(UUID owner, String symbol, Instant since) throws SQLException {
    try (Connection c = source.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(shares), 0) FROM sm_ledger "
            + "WHERE owner = ? AND symbol = ? AND created_at >= ?")) {
      ps.setString(1, owner.toString());
      ps.setString(2, symbol);
      ps.setLong(3, since.toEpochMilli());
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getLong(1) : 0;
      }
    }
  }

  /** Newest first. Pass null to skip a filter. */
  public List<LedgerEntry> ledger(UUID owner, String symbol, int limit) throws SQLException {
    StringBuilder sql = new StringBuilder("SELECT owner, type, symbol, shares, price, fee, cash, "
        + "created_at, reference FROM sm_ledger WHERE 1 = 1");
    if (owner != null) {
      sql.append(" AND owner = ?");
    }
    if (symbol != null) {
      sql.append(" AND symbol = ?");
    }
    sql.append(" ORDER BY created_at DESC, id DESC LIMIT ?");

    List<LedgerEntry> entries = new ArrayList<>();
    try (Connection c = source.getConnection();
        PreparedStatement ps = c.prepareStatement(sql.toString())) {
      int i = 1;
      if (owner != null) {
        ps.setString(i++, owner.toString());
      }
      if (symbol != null) {
        ps.setString(i++, symbol);
      }
      ps.setInt(i, limit);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          entries.add(new LedgerEntry(UUID.fromString(rs.getString(1)),
              EntryType.valueOf(rs.getString(2)), rs.getString(3), rs.getLong(4),
              rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7),
              Instant.ofEpochMilli(rs.getLong(8)), rs.getString(9)));
        }
      }
    }
    return entries;
  }

  public List<Order> loadOrders() throws SQLException {
    List<Order> orders = new ArrayList<>();
    try (Connection c = source.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT id, owner, symbol, type, shares, "
            + "target, created_at, expires_at FROM sm_orders ORDER BY id");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        orders.add(new Order(rs.getLong(1), UUID.fromString(rs.getString(2)), rs.getString(3),
            OrderType.valueOf(rs.getString(4)), rs.getLong(5), rs.getBigDecimal(6),
            Instant.ofEpochMilli(rs.getLong(7)), Instant.ofEpochMilli(rs.getLong(8))));
      }
    }
    return orders;
  }

  /** Saves a new order and returns it with its id. */
  public Order insertOrder(Order order) throws SQLException {
    try (Connection c = source.getConnection();
        PreparedStatement ps = c.prepareStatement("INSERT INTO sm_orders (owner, symbol, type, "
            + "shares, target, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, order.owner().toString());
      ps.setString(2, order.symbol());
      ps.setString(3, order.type().name());
      ps.setLong(4, order.shares());
      ps.setBigDecimal(5, order.target());
      ps.setLong(6, order.created().toEpochMilli());
      ps.setLong(7, order.expires().toEpochMilli());
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) {
          throw new SQLException("No id returned for the new order");
        }
        return order.withId(keys.getLong(1));
      }
    }
  }

  public void deleteOrder(long id) throws SQLException {
    try (Connection c = source.getConnection();
        PreparedStatement ps = c.prepareStatement("DELETE FROM sm_orders WHERE id = ?")) {
      ps.setLong(1, id);
      ps.executeUpdate();
    }
  }

  public Optional<String> meta(String name) throws SQLException {
    try (Connection c = source.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT value FROM sm_meta WHERE name = ?")) {
      ps.setString(1, name);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
      }
    }
  }

  public void setMeta(String name, String value) throws SQLException {
    try (Connection c = source.getConnection()) {
      c.setAutoCommit(false);
      try (PreparedStatement delete = c.prepareStatement("DELETE FROM sm_meta WHERE name = ?");
          PreparedStatement insert = c.prepareStatement(
              "INSERT INTO sm_meta (name, value) VALUES (?, ?)")) {
        delete.setString(1, name);
        delete.executeUpdate();
        insert.setString(1, name);
        insert.setString(2, value);
        insert.executeUpdate();
        c.commit();
      } catch (SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }

  private static void writeHoldings(Connection c, Collection<Holding> holdings)
      throws SQLException {
    try (PreparedStatement delete = c.prepareStatement(
            "DELETE FROM sm_holdings WHERE owner = ? AND symbol = ?");
        PreparedStatement insert = c.prepareStatement(
            "INSERT INTO sm_holdings (owner, symbol, shares, cost_basis) VALUES (?, ?, ?, ?)")) {
      for (Holding holding : holdings) {
        delete.setString(1, holding.owner().toString());
        delete.setString(2, holding.symbol());
        delete.executeUpdate();
        if (holding.shares() > 0) {
          insert.setString(1, holding.owner().toString());
          insert.setString(2, holding.symbol());
          insert.setLong(3, holding.shares());
          insert.setBigDecimal(4, holding.costBasis());
          insert.executeUpdate();
        }
      }
    }
  }

  private static void writeEntries(Connection c, Collection<LedgerEntry> entries)
      throws SQLException {
    try (PreparedStatement insert = c.prepareStatement("INSERT INTO sm_ledger (owner, type, "
        + "symbol, shares, price, fee, cash, created_at, reference) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
      for (LedgerEntry entry : entries) {
        insert.setString(1, entry.owner().toString());
        insert.setString(2, entry.type().name());
        insert.setString(3, entry.symbol());
        insert.setLong(4, entry.shares());
        if (entry.price() == null) {
          insert.setNull(5, Types.DECIMAL);
        } else {
          insert.setBigDecimal(5, entry.price());
        }
        insert.setBigDecimal(6, entry.fee() == null ? BigDecimal.ZERO : entry.fee());
        insert.setBigDecimal(7, entry.cash() == null ? BigDecimal.ZERO : entry.cash());
        insert.setLong(8, entry.time().toEpochMilli());
        insert.setString(9, entry.reference());
        insert.addBatch();
      }
      insert.executeBatch();
    }
  }

  @Override
  public void close() {
    source.close();
  }
}
