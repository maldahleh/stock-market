package com.maldahleh.stockmarket.storage.types;

import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SQLite implements Storage {

  private static final String SQLITE_URL = "jdbc:sqlite:plugins/StockMarket/StockMarket.db";

  private static final String CREATE_QUERY =
      "CREATE TABLE IF NOT EXISTS "
          + "sm_transactions(id INTEGER PRIMARY KEY, uuid CHAR(36), tran_type VARCHAR(8), "
          + "tran_date DATETIME, symbol VARCHAR(12), quantity INTEGER, single_price VARCHAR(20), "
          + "broker_fee VARCHAR(20), earnings VARCHAR(20), sold BOOLEAN)";
  private static final String PURCHASE_QUERY =
      "INSERT INTO sm_transactions (id, uuid, tran_type, "
          + "tran_date, symbol, quantity, single_price, broker_fee) VALUES (?, ?, 'purchase', "
          + "?, ?, ?, ?, ?)";
  private static final String SALE_QUERY =
      "INSERT INTO sm_transactions (id, uuid, tran_type, tran_date, symbol, quantity, single_price,"
          + " broker_fee, earnings) VALUES (?, ?, 'sale', ?, ?, ?, ?, ?, ?)";
  private static final String MARK_SOLD = "UPDATE sm_transactions SET sold = true WHERE id = ?";
  private static final String GET_QUERY =
      "SELECT id, tran_type, tran_date, symbol, quantity, "
          + "single_price, broker_fee, earnings, sold FROM sm_transactions WHERE uuid = ? "
          + "ORDER BY tran_date";
  private static final String GET_HISTORY_QUERY =
      "SELECT id, uuid, tran_type, tran_date, symbol, "
          + "quantity, single_price, broker_fee, earnings, sold FROM sm_transactions ORDER BY "
          + "tran_date LIMIT 100";
  private static final String STOCK_QUERY =
      "SELECT id, uuid, tran_type, tran_date, symbol, quantity, single_price, broker_fee, earnings,"
          + " sold FROM sm_transactions WHERE symbol = ? ORDER BY tran_date";
  private static final String GET_LAST_QUERY = "SELECT MAX(id) FROM sm_transactions";

  private int currentId;

  public SQLite() {
    createTables();
    currentId = getLastId();
  }

  private void createTables() {
    try (Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(CREATE_QUERY)) {
      statement.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private int getLastId() {
    try (Connection connection = getConnection();
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
    try (Connection connection = getConnection();
        PreparedStatement statement = getActionStatement(connection, transaction, true)) {
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void processSale(Transaction transaction) {
    try (Connection connection = getConnection();
        PreparedStatement statement = getActionStatement(connection, transaction, false)) {
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void markSold(Transaction transaction) {
    try (Connection connection = getConnection();
        PreparedStatement statement = getMarkSoldStatement(connection, transaction)) {
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public List<Transaction> getPlayerTransactions(UUID uuid) {
    List<Transaction> transactions = new ArrayList<>();

    try (Connection connection = getConnection();
        PreparedStatement statement = getPlayerGetStatement(connection, uuid);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        BigDecimal earnings = null;

        String earningsString = resultSet.getString(8);
        if (earningsString != null) {
          earnings = new BigDecimal(earningsString);
        }

        transactions.add(
            new Transaction(
                resultSet.getInt(1),
                uuid,
                TransactionType.valueOf(resultSet.getString(2)),
                resultSet.getTimestamp(3).toInstant(),
                resultSet.getString(4),
                resultSet.getInt(5),
                new BigDecimal(resultSet.getString(6)),
                new BigDecimal(resultSet.getString(7)),
                earnings,
                null,
                null,
                resultSet.getBoolean(9)));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return transactions;
  }

  @Override
  public List<Transaction> getStockTransactions(String symbol) {
    List<Transaction> transactions = new ArrayList<>();

    try (Connection connection = getConnection();
        PreparedStatement statement = getStockGetStatement(connection, symbol);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        BigDecimal earnings = null;

        String earningsString = resultSet.getString(9);
        if (earningsString != null) {
          earnings = new BigDecimal(earningsString);
        }

        transactions.add(
            new Transaction(
                resultSet.getInt(1),
                UUID.fromString(resultSet.getString(2)),
                TransactionType.valueOf(resultSet.getString(3)),
                resultSet.getTimestamp(4).toInstant(),
                resultSet.getString(5),
                resultSet.getInt(6),
                new BigDecimal(resultSet.getString(7)),
                new BigDecimal(resultSet.getString(8)),
                earnings,
                null,
                null,
                resultSet.getBoolean(10)));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return transactions;
  }

  @Override
  public List<Transaction> getTransactionHistory() {
    List<Transaction> transactions = new ArrayList<>();

    try (Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(GET_HISTORY_QUERY);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        BigDecimal earnings = null;

        String earningsString = resultSet.getString(9);
        if (earningsString != null) {
          earnings = new BigDecimal(earningsString);
        }

        transactions.add(
            new Transaction(
                resultSet.getInt(1),
                UUID.fromString(resultSet.getString(2)),
                TransactionType.valueOf(resultSet.getString(3)),
                resultSet.getTimestamp(4).toInstant(),
                resultSet.getString(5),
                resultSet.getInt(6),
                new BigDecimal(resultSet.getString(7)),
                new BigDecimal(resultSet.getString(8)),
                earnings,
                null,
                null,
                resultSet.getBoolean(10)));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return transactions;
  }

  private PreparedStatement getActionStatement(
      Connection connection, Transaction transaction, boolean isPurchase) throws SQLException {
    PreparedStatement statement =
        connection.prepareStatement(isPurchase ? PURCHASE_QUERY : SALE_QUERY);
    statement.setInt(1, transaction.getId());
    statement.setString(2, transaction.getUuid().toString());
    statement.setTimestamp(3, Timestamp.from(transaction.getTransactionDate()));
    statement.setString(4, transaction.getSymbol().toUpperCase());
    statement.setInt(5, transaction.getQuantity());
    statement.setString(6, transaction.getSinglePrice().toPlainString());
    statement.setString(7, transaction.getBrokerFee().toPlainString());
    if (transaction.getEarnings() != null) {
      statement.setString(8, transaction.getEarnings().toPlainString());
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

  private Connection getConnection() throws SQLException {
    return DriverManager.getConnection(SQLITE_URL);
  }
}
