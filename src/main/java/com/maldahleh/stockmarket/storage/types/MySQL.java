package com.maldahleh.stockmarket.storage.types;

import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;

public class MySQL implements Storage {

  private static final String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS "
      + "sm_transactions(id INTEGER PRIMARY KEY, uuid CHAR(36), tran_type ENUM('purchase', 'sale'), "
      + "tran_date DATETIME, symbol VARCHAR(12), quantity INTEGER, single_price DECIMAL(19, 2), "
      + "broker_fee DECIMAL(19, 2), earnings DECIMAL(19, 2), sold BOOLEAN)";
  private static final String PURCHASE_QUERY = "INSERT INTO sm_transactions (id, uuid, tran_type, "
      + "tran_date, symbol, quantity, single_price, broker_fee) VALUES (?, ?, 'purchase', "
      + "?, ?, ?, ?, ?)";
  private static final String SALE_QUERY = "INSERT INTO sm_transactions (id, uuid, tran_type, "
      + "tran_date, symbol, quantity, single_price, broker_fee, earnings) VALUES (?, ?, 'sale', "
      + "?, ?, ?, ?, ?, ?)";
  private static final String MARK_SOLD = "UPDATE sm_transactions SET sold = true WHERE id = ?";
  private static final String GET_QUERY = "SELECT id, tran_type, tran_date, symbol, quantity, "
      + "single_price, broker_fee, earnings, sold FROM sm_transactions WHERE uuid = ? "
      + "ORDER BY tran_date";
  private static final String GET_HISTORY_QUERY = "SELECT id, uuid, tran_type, tran_date, symbol, "
      + "quantity, single_price, broker_fee, earnings, sold FROM sm_transactions ORDER BY "
      + "tran_date LIMIT 100";
  private static final String STOCK_QUERY = "SELECT id, uuid, tran_type, tran_date, symbol, "
      + "quantity, single_price, broker_fee, earnings, sold FROM sm_transactions WHERE symbol = ? "
      + "ORDER BY tran_date";
  private static final String GET_LAST_QUERY = "SELECT MAX(id) FROM sm_transactions";

  private final HikariDataSource pool;
  private int currentId;

  public MySQL(ConfigurationSection section) {
    HikariConfig config = new HikariConfig();

    config.setDriverClassName("com.mysql.jdbc.Driver");
    config.setJdbcUrl("jdbc:mysql://" + section.getString("ip") + ":" + section.getInt("port")
        + "/" + section.getString("database"));
    config.setUsername(section.getString("username"));
    config.setPassword(section.getString("password"));

    config.setMaximumPoolSize(section.getInt("max-pool-size", 100));
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "400");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("useServerPrepStmts", "true");

    pool = new HikariDataSource(config);
    createTables();

    currentId = getLastId();
  }

  private void createTables() {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement(CREATE_QUERY)) {
      statement.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private int getLastId() {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement(GET_LAST_QUERY);
        ResultSet resultSet = statement.executeQuery()) {
      if (resultSet.next()) {
        return resultSet.getInt(1);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return -1;
  }

  @Override
  public int getNextId() {
    currentId++;
    return currentId;
  }

  @Override
  public void processPurchase(Transaction transaction) {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = getActionStatement(connection, transaction, true)) {
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void processSale(Transaction transaction) {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = getActionStatement(connection, transaction, false)) {
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void markSold(Transaction transaction) {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = getMarkSoldStatement(connection, transaction)) {
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public List<Transaction> getPlayerTransactions(UUID uuid) {
    List<Transaction> transactions = new ArrayList<>();

    try (Connection connection = pool.getConnection();
        PreparedStatement statement = getPlayerGetStatement(connection, uuid);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        transactions.add(new Transaction(resultSet.getInt(1), uuid,
            resultSet.getString(2).toUpperCase(),
            resultSet.getTimestamp(3).toInstant(), resultSet.getString(4),
            resultSet.getInt(5), resultSet.getBigDecimal(6),
            resultSet.getBigDecimal(7), resultSet.getBigDecimal(8),
            null, null, resultSet.getBoolean(9)));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return transactions;
  }

  @Override
  public List<Transaction> getStockTransactions(String symbol) {
    List<Transaction> transactions = new ArrayList<>();

    try (Connection connection = pool.getConnection();
        PreparedStatement statement = getStockGetStatement(connection, symbol);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        transactions.add(new Transaction(resultSet.getInt(1), UUID.fromString(resultSet
            .getString(2)), resultSet.getString(3).toUpperCase(),
            resultSet.getTimestamp(4).toInstant(), resultSet.getString(5),
            resultSet.getInt(6), resultSet.getBigDecimal(7),
            resultSet.getBigDecimal(8), resultSet.getBigDecimal(9),
            null, null, resultSet.getBoolean(10)));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return transactions;
  }

  @Override
  public List<Transaction> getTransactionHistory() {
    List<Transaction> transactions = new ArrayList<>();

    try (Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement(GET_HISTORY_QUERY);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        transactions.add(new Transaction(resultSet.getInt(1), UUID.fromString(resultSet
            .getString(2)), resultSet.getString(3).toUpperCase(),
            resultSet.getTimestamp(4).toInstant(), resultSet.getString(5),
            resultSet.getInt(6), resultSet.getBigDecimal(7),
            resultSet.getBigDecimal(8), resultSet.getBigDecimal(9),
            null, null, resultSet.getBoolean(10)));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return transactions;
  }

  private PreparedStatement getActionStatement(Connection connection, Transaction transaction,
      boolean isPurchase) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(isPurchase ? PURCHASE_QUERY
        : SALE_QUERY);
    statement.setInt(1, transaction.getId());
    statement.setString(2, transaction.getUuid().toString());
    statement.setTimestamp(3, Timestamp.from(transaction.getTransactionDate()));
    statement.setString(4, transaction.getSymbol().toUpperCase());
    statement.setInt(5, transaction.getQuantity());
    statement.setBigDecimal(6, transaction.getSinglePrice());
    statement.setBigDecimal(7, transaction.getBrokerFee());
    if (transaction.getEarnings() != null) {
      statement.setBigDecimal(8, transaction.getEarnings());
    }

    return statement;
  }

  private PreparedStatement getMarkSoldStatement(Connection connection, Transaction transaction)
      throws SQLException {
    PreparedStatement statement = connection.prepareStatement(MARK_SOLD);
    statement.setInt(1, transaction.getId());

    return statement;
  }

  private PreparedStatement getPlayerGetStatement(Connection connection, UUID uuid)
      throws SQLException {
    PreparedStatement statement = connection.prepareStatement(GET_QUERY);
    statement.setString(1, uuid.toString());

    return statement;
  }

  private PreparedStatement getStockGetStatement(Connection connection, String symbol)
      throws SQLException {
    PreparedStatement statement = connection.prepareStatement(STOCK_QUERY);
    statement.setString(1, symbol);

    return statement;
  }
}