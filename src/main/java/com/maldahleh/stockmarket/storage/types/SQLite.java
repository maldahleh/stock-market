package com.maldahleh.stockmarket.storage.types;

import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import com.zaxxer.hikari.HikariConfig;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;

public class SQLite extends Storage {

  public SQLite(ConfigurationSection section) {
    super(section);
  }

  @Override
  protected HikariConfig buildHikariConfig(ConfigurationSection section) {
    HikariConfig config = new HikariConfig();
    config.setPoolName("AuthMeSQLitePool");
    config.setDriverClassName("org.sqlite.JDBC");
    config.setJdbcUrl("jdbc:sqlite:plugins/StockMarket/StockMarket.db");
    config.setConnectionTestQuery("SELECT 1");
    config.setMaxLifetime(60000);
    config.setIdleTimeout(45000);
    config.setMaximumPoolSize(50);

    return config;
  }

  @Override
  protected String getCreateTableQuery() {
    return "CREATE TABLE IF NOT EXISTS sm_transactions(id INTEGER PRIMARY KEY, uuid CHAR(36), "
        + "tran_type VARCHAR(8), tran_date DATETIME, symbol VARCHAR(12), quantity INTEGER, "
        + "single_price VARCHAR(20), broker_fee VARCHAR(20), earnings VARCHAR(20), sold BOOLEAN)";
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
