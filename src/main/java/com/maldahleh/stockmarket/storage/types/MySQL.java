package com.maldahleh.stockmarket.storage.types;

import com.maldahleh.stockmarket.config.models.SqlSettings;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import com.zaxxer.hikari.HikariConfig;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;

public class MySQL extends Storage {

  public MySQL(SqlSettings settings) {
    super(settings);
  }

  @Override
  protected HikariConfig buildHikariConfig(SqlSettings settings) {
    HikariConfig config = new HikariConfig();
    config.setDriverClassName("com.mysql.jdbc.Driver");
    config.setJdbcUrl(
        "jdbc:mysql://"
            + settings.getIp()
            + ":"
            + settings.getPort()
            + "/"
            + settings.getDatabase());
    config.setUsername(settings.getUsername());
    config.setPassword(settings.getPassword());

    config.setMaximumPoolSize(settings.getMaxPoolSize());
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "400");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("useServerPrepStmts", "true");

    return config;
  }

  @Override
  protected String getCreateTableQuery() {
    return "CREATE TABLE IF NOT EXISTS sm_transactions(id INTEGER PRIMARY KEY AUTO_INCREMENT, "
        + "uuid CHAR(36), tran_type ENUM('purchase', 'sale'), tran_date DATETIME, "
        + "symbol VARCHAR(12), quantity INTEGER, single_price DECIMAL(19, 2), "
        + "broker_fee DECIMAL(19, 2), earnings DECIMAL(19, 2), sold BOOLEAN)";
  }

  @Override
  protected String getLastInsertQuery() {
    return "SELECT LAST_INSERT_ID();";
  }

  @Override
  protected Transaction buildTransaction(ResultSet resultSet) throws SQLException {
    return new Transaction(
        resultSet.getInt(1),
        UUID.fromString(resultSet.getString(2)),
        TransactionType.valueOf(resultSet.getString(3)),
        resultSet.getTimestamp(4).toInstant(),
        resultSet.getString(5),
        resultSet.getInt(6),
        resultSet.getBigDecimal(7),
        resultSet.getBigDecimal(8),
        resultSet.getBigDecimal(9),
        null,
        null,
        resultSet.getBoolean(10)
    );
  }
}
