package com.maldahleh.stockmarket.storage.types;

import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
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

  private static final String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS "
      + "sm_transactions(uuid CHAR(36), tran_type VARCHAR(8), tran_date DATETIME, "
      + "symbol VARCHAR(12), quantity INTEGER, single_price VARCHAR(20), broker_fee VARCHAR(20), "
      + "earnings VARCHAR(20), sold BOOLEAN)";
  private static final String PURCHASE_QUERY = "INSERT INTO sm_transactions (uuid, tran_type, "
      + "tran_date, symbol, quantity, single_price, broker_fee) VALUES (?, 'purchase', "
      + "?, ?, ?, ?, ?)";
  private static final String SALE_QUERY = "INSERT INTO sm_transactions (uuid, tran_type, "
      + "tran_date, symbol, quantity, single_price, broker_fee, earnings) VALUES (?, 'sale', "
      + "?, ?, ?, ?, ?, ?)";
  private static final String MARK_SOLD = "UPDATE sm_transactions SET sold = true WHERE uuid = ? "
      + "AND tran_type = 'purchase' AND tran_date = ? AND symbol = ? AND quantity = ? "
      + "AND single_price = ? AND broker_fee = ?";
  private static final String GET_QUERY = "SELECT tran_type, tran_date, symbol, quantity, "
      + "single_price, broker_fee, earnings, sold FROM sm_transactions WHERE uuid = ? "
      + "ORDER BY tran_date";
  private static final String STOCK_QUERY = "SELECT uuid, tran_type, tran_date, symbol, quantity, "
      + "single_price, broker_fee, earnings, sold FROM sm_transactions WHERE symbol = ? ORDER BY "
      + "tran_date";

  public SQLite() {
    createTables();
  }

  private void createTables() {
    try (Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(CREATE_QUERY)) {
      statement.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
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
  public void markSold(UUID uuid, Transaction transaction) {
    try (Connection connection = getConnection();
        PreparedStatement statement = getMarkSoldStatement(connection, uuid, transaction)) {
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

        String earningsString = resultSet.getString(7);
        if (earningsString != null) {
          earnings = new BigDecimal(earningsString);
        }

        transactions.add(new Transaction(uuid, resultSet.getString(1).toUpperCase(),
            resultSet.getTimestamp(2).toInstant(), resultSet.getString(3),
            resultSet.getInt(4), new BigDecimal(resultSet.getString(5)),
            new BigDecimal(resultSet.getString(6)), earnings, null, null,
            resultSet.getBoolean(8)));
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

        String earningsString = resultSet.getString(8);
        if (earningsString != null) {
          earnings = new BigDecimal(earningsString);
        }

        transactions.add(new Transaction(UUID.fromString(resultSet.getString(1)),
            resultSet.getString(2).toUpperCase(),
            resultSet.getTimestamp(3).toInstant(), resultSet.getString(4),
            resultSet.getInt(5), new BigDecimal(resultSet.getString(6)),
            new BigDecimal(resultSet.getString(7)), earnings, null, null,
            resultSet.getBoolean(9)));
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
    statement.setString(5, transaction.getSinglePrice().toPlainString());
    statement.setString(6, transaction.getBrokerFee().toPlainString());
    if (transaction.getEarnings() != null) {
      statement.setString(7, transaction.getEarnings().toPlainString());
    }

    return statement;
  }

  private PreparedStatement getMarkSoldStatement(Connection connection, UUID uuid,
      Transaction transaction) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(MARK_SOLD);
    statement.setString(1, uuid.toString());
    statement.setTimestamp(2, Timestamp.from(transaction.getTransactionDate()));
    statement.setString(3, transaction.getSymbol());
    statement.setInt(4, transaction.getQuantity());
    statement.setString(5, transaction.getSinglePrice().toPlainString());
    statement.setString(6, transaction.getBrokerFee().toPlainString());

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