package com.maldahleh.stockmarket.storage;

import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
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

@SuppressWarnings("java:S2095") // use prepared statement in try/finally
public abstract class Storage extends StorageStatements {

  private final HikariDataSource pool;

  protected abstract HikariConfig buildHikariConfig(ConfigurationSection section);

  protected abstract Transaction buildTransaction(ResultSet resultSet) throws SQLException;

  protected Storage(ConfigurationSection section) {
    pool = new HikariDataSource(buildHikariConfig(section));
    createTable();
  }

  public void processTransaction(Transaction transaction) {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = buildActionStatement(connection, transaction);
        ResultSet resultSet = statement.executeQuery()) {
      if (resultSet.next()) {
        transaction.setId(resultSet.getInt(1));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public List<Transaction> getTransactionHistory() {
    List<Transaction> transactions = new ArrayList<>();

    try (Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement(getRecentTransactionsQuery());
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        transactions.add(buildTransaction(resultSet));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return transactions;
  }

  public List<Transaction> getPlayerTransactions(UUID uuid) {
    List<Transaction> transactions = new ArrayList<>();

    try (Connection connection = pool.getConnection();
        PreparedStatement statement = buildPlayerTransactionStatement(connection, uuid);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        transactions.add(buildTransaction(resultSet));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return transactions;
  }

  public List<Transaction> getStockTransactions(String symbol) {
    List<Transaction> transactions = new ArrayList<>();

    try (Connection connection = pool.getConnection();
        PreparedStatement statement = buildStockTransactionStatement(connection, symbol);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        transactions.add(buildTransaction(resultSet));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return transactions;
  }

  public void markSold(Transaction transaction) {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = buildMarkSoldStatement(connection, transaction)) {
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void createTable() {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement(getCreateTableQuery())) {
      statement.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private PreparedStatement buildActionStatement(Connection connection, Transaction transaction)
      throws SQLException {
    PreparedStatement statement = connection.prepareStatement(getActionQuery(transaction));
    statement.setString(1, transaction.getUuid().toString());
    statement.setTimestamp(2, Timestamp.from(transaction.getTransactionDate()));
    statement.setString(3, transaction.getSymbol().toUpperCase());
    statement.setInt(4, transaction.getQuantity());
    statement.setString(5, transaction.getSinglePrice().toPlainString());
    statement.setString(6, transaction.getBrokerFee().toPlainString());
    if (transaction.getEarnings() != null) {
      statement.setString(7, transaction.getEarnings().toPlainString());
    }

    return statement;
  }

  private String getActionQuery(Transaction transaction) {
    if (transaction.getTransactionType() == TransactionType.SALE) {
      return getSaleQuery();
    } else {
      return getPurchaseQuery();
    }
  }

  protected PreparedStatement buildPlayerTransactionStatement(Connection connection, UUID uuid)
      throws SQLException {
    PreparedStatement statement = connection.prepareStatement(getPlayerTransactionsQuery());
    statement.setString(1, uuid.toString());

    return statement;
  }

  private PreparedStatement buildStockTransactionStatement(Connection connection, String symbol)
      throws SQLException {
    PreparedStatement statement = connection.prepareStatement(getStockTransactionsQuery());
    statement.setString(1, symbol);

    return statement;
  }

  protected PreparedStatement buildMarkSoldStatement(Connection connection, Transaction transaction)
      throws SQLException {
    PreparedStatement statement = connection.prepareStatement(getMarkSoldQuery());
    statement.setInt(1, transaction.getId());

    return statement;
  }
}
