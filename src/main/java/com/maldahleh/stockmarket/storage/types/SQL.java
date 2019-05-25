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

public class SQL implements Storage {
  private static final String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS "
      + "sm_transactions(uuid CHAR(36), tran_type ENUM('purchase', 'sale'), "
      + "tran_date DATETIME, symbol VARCHAR(12), quantity INTEGER, single_price DECIMAL, "
      + "broker_fee DECIMAL, earnings DECIMAL)";
  private static final String PURCHASE_QUERY = "INSERT INTO sm_transactions (uuid, tran_type, "
      + "tran_date, symbol, quantity, single_price, broker_fee) VALUES (?, 'purchase', "
      + "?, ?, ?, ?, ?)";
  private static final String SALE_QUERY = "INSERT INTO sm_transactions (uuid, tran_type, "
      + "tran_date, symbol, quantity, single_price, broker_fee, earnings) VALUES (?, 'sale', "
      + "?, ?, ?, ?, ?, ?)";
  private static final String GET_QUERY = "SELECT tran_type, tran_date, symbol, quantity, "
      + "single_price, broker_fee, earnings FROM sm_transactions WHERE uuid = ? ORDER BY tran_date";
  private static final String STOCK_QUERY = "SELECT uuid, tran_type, tran_date, symbol, quantity, "
      + "single_price, broker_fee, earnings FROM sm_transactions WHERE symbol = ? ORDER BY "
      + "tran_date";

  private final HikariDataSource pool;

  public SQL(ConfigurationSection section) {
    HikariConfig config = new HikariConfig();

    config.setDriverClassName("com.mysql.jdbc.Driver");
    config.setJdbcUrl("jdbc:mysql://" + section.getString("ip") + ":" + section.getInt("port")
        + "/" + section.getString("database"));
    config.setUsername(section.getString("username"));
    config.setPassword(section.getString("password"));

    config.setMaximumPoolSize(200);
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "400");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("useServerPrepStmts", "true");

    pool = new HikariDataSource(config);
    createTables();
  }

  private void createTables() {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement(CREATE_QUERY)) {
      statement.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void processPurchase(Transaction transaction) {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = getActionStatement(connection, transaction, true)) {
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void processSale(Transaction transaction) {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = getActionStatement(connection, transaction, false)) {
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public List<Transaction> getPlayerTransactions(UUID uuid) {
    List<Transaction> transactions = new ArrayList<>();

    try (Connection connection = pool.getConnection();
        PreparedStatement statement = getPlayerGetStatement(connection, uuid);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        transactions.add(new Transaction(uuid, resultSet.getString(1).toUpperCase(),
            resultSet.getTimestamp(2).toInstant(), resultSet.getString(3),
            resultSet.getInt(4), resultSet.getBigDecimal(5),
            resultSet.getBigDecimal(6), resultSet.getBigDecimal(7)));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return transactions;
  }

  public List<Transaction> getStockTransactions(String symbol) {
    List<Transaction> transactions = new ArrayList<>();

    try (Connection connection = pool.getConnection();
        PreparedStatement statement = getStockGetStatement(connection, symbol);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        transactions.add(new Transaction(UUID.fromString(resultSet.getString(1)),
            resultSet.getString(2).toUpperCase(),
            resultSet.getTimestamp(3).toInstant(), resultSet.getString(4),
            resultSet.getInt(5), resultSet.getBigDecimal(6),
            resultSet.getBigDecimal(7), resultSet.getBigDecimal(8)));
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
    statement.setString(1, transaction.getUuid().toString());
    statement.setTimestamp(2, Timestamp.from(transaction.getTransactionDate()));
    statement.setString(3, transaction.getSymbol().toUpperCase());
    statement.setInt(4, transaction.getQuantity());
    statement.setBigDecimal(5, transaction.getSinglePrice());
    statement.setBigDecimal(6, transaction.getBrokerFee());
    if (transaction.getEarnings() != null) {
      statement.setBigDecimal(7, transaction.getEarnings());
    }

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