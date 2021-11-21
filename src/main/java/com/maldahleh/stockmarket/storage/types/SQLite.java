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
        + "tran_type VARCHAR(8), tran_date DATETIME, symbol VARCHAR(12), quantity INTEGER, "
        + "single_price VARCHAR(20), broker_fee VARCHAR(20), earnings VARCHAR(20), sold BOOLEAN)";
  }

  @Override
  protected String getLastInsertQuery() {
    return "SELECT last_insert_rowid();";
  }

  @Override
  protected Transaction buildTransaction(ResultSet resultSet)
      throws SQLException {
    BigDecimal earnings = null;

    String earningsString = resultSet.getString(9);
    if (earningsString != null) {
      earnings = new BigDecimal(earningsString);
    }

    return new Transaction(
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
        resultSet.getBoolean(10)
    );
  }
}
