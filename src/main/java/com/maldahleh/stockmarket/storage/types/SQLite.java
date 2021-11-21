package com.maldahleh.stockmarket.storage.types;

import com.maldahleh.stockmarket.config.models.SqlSettings;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import com.zaxxer.hikari.HikariConfig;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SQLite extends Storage {

  private static final int MAXIMUM_POOL_SIZE = 50;

  private static final long MAX_LIFETIME = TimeUnit.MINUTES.toMillis(1);
  private static final long IDLE_TIMEOUT = TimeUnit.SECONDS.toMillis(45);

  public SQLite(SqlSettings settings) {
    super(settings);
  }

  @Override
  protected HikariConfig buildHikariConfig(SqlSettings settings) {
    HikariConfig config = new HikariConfig();
    config.setPoolName("StockMarketPool");
    config.setDriverClassName("org.sqlite.JDBC");
    config.setJdbcUrl("jdbc:sqlite:plugins/StockMarket/StockMarket.db");
    config.setConnectionTestQuery("SELECT 1");

    config.setMaxLifetime(MAX_LIFETIME);
    config.setIdleTimeout(IDLE_TIMEOUT);
    config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);

    return config;
  }

  @Override
  protected String getCreateTableQuery() {
    return "CREATE TABLE IF NOT EXISTS sm_transactions(id INTEGER PRIMARY KEY, uuid CHAR(36), "
        + "type VARCHAR(8), date DATETIME, symbol VARCHAR(12), quantity INTEGER, single_price "
        + "VARCHAR(20), broker_fee VARCHAR(20), earnings VARCHAR(20), sold BOOLEAN)";
  }

  @Override
  protected String getLastInsertQuery() {
    return "SELECT last_insert_rowid();";
  }

  @Override
  protected Transaction buildTransaction(ResultSet resultSet)
      throws SQLException {
    BigDecimal earnings = null;

    String earningsString = resultSet.getString("earnings");
    if (earningsString != null) {
      earnings = new BigDecimal(earningsString);
    }

    return Transaction.builder()
        .id(resultSet.getInt("id"))
        .uuid(UUID.fromString(resultSet.getString("uuid")))
        .transactionType(TransactionType.valueOf(resultSet.getString("type")))
        .transactionDate(resultSet.getTimestamp("date").toInstant())
        .symbol(resultSet.getString("symbol"))
        .quantity(resultSet.getInt("quantity"))
        .singlePrice(new BigDecimal(resultSet.getString("single_price")))
        .brokerFee(new BigDecimal(resultSet.getString("broker_fee")))
        .earnings(earnings)
        .sold(resultSet.getBoolean("sold"))
        .build();
  }
}
