package com.maldahleh.stockmarket.storage.types;

import com.maldahleh.stockmarket.config.models.SqlSettings;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import com.zaxxer.hikari.HikariConfig;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MySQL extends Storage {

  private static final int MAXIMUM_POOL_SIZE = (Runtime.getRuntime().availableProcessors() * 2) + 1;
  private static final int MINIMUM_IDLE = Math.min(MAXIMUM_POOL_SIZE, 10);

  private static final long MAX_LIFETIME = TimeUnit.MINUTES.toMillis(30);
  private static final long LEAK_DETECTION_THRESHOLD = TimeUnit.SECONDS.toMillis(10);
  private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
  private static final long SOCKET_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

  public MySQL(SqlSettings settings) {
    super(settings);
  }

  @Override
  protected HikariConfig buildHikariConfig(SqlSettings settings) {
    HikariConfig config = new HikariConfig();
    config.setPoolName("StockMarketPool");
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

    config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
    config.setMinimumIdle(MINIMUM_IDLE);

    config.setMaxLifetime(MAX_LIFETIME);
    config.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);
    config.setConnectionTimeout(CONNECTION_TIMEOUT);

    config.addDataSourceProperty("socketTimeout", String.valueOf(SOCKET_TIMEOUT));

    config.addDataSourceProperty("useUnicode", "true");
    config.addDataSourceProperty("characterEncoding", "utf8");

    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("useServerPrepStmts", "true");
    config.addDataSourceProperty("useLocalSessionState", "true");
    config.addDataSourceProperty("rewriteBatchedStatements", "true");
    config.addDataSourceProperty("cacheServerConfiguration", "true");
    config.addDataSourceProperty("cacheResultSetMetadata", "true");
    config.addDataSourceProperty("elideSetAutoCommits", "true");
    config.addDataSourceProperty("maintainTimeStats", "false");
    config.addDataSourceProperty("alwaysSendSetIsolation", "false");
    config.addDataSourceProperty("cacheCallableStmts", "true");

    return config;
  }

  @Override
  protected String getCreateTableQuery() {
    return "CREATE TABLE IF NOT EXISTS sm_transactions(id INTEGER PRIMARY KEY AUTO_INCREMENT, "
        + "uuid CHAR(36), type ENUM('purchase', 'sale'), date DATETIME, "
        + "symbol VARCHAR(12), quantity INTEGER, single_price DECIMAL(19, 2), "
        + "broker_fee DECIMAL(19, 2), earnings DECIMAL(19, 2), sold BOOLEAN)";
  }

  @Override
  protected String getLastInsertQuery() {
    return "SELECT LAST_INSERT_ID();";
  }

  @Override
  protected Transaction buildTransaction(ResultSet resultSet) throws SQLException {
    return Transaction.builder()
        .id(resultSet.getInt("id"))
        .uuid(UUID.fromString(resultSet.getString("uuid")))
        .type(TransactionType.valueOf(resultSet.getString("type")))
        .date(resultSet.getTimestamp("date").toInstant())
        .symbol(resultSet.getString("symbol"))
        .quantity(resultSet.getInt("quantity"))
        .singlePrice(resultSet.getBigDecimal("single_price"))
        .brokerFee(resultSet.getBigDecimal("broker_fee"))
        .earnings(resultSet.getBigDecimal("earnings"))
        .sold(resultSet.getBoolean("sold"))
        .build();
  }
}
