package com.maldahleh.stockmarket.storage;

@SuppressWarnings("java:S3400") // use constant instead of method
public abstract class StorageStatements {

  private static final String SELECT_ALL = "SELECT id, uuid, tran_type, tran_date, symbol, "
      + "quantity, single_price, broker_fee, earnings, sold FROM sm_transactions";

  protected abstract String getCreateTableQuery();

  protected abstract String getLastInsertQuery();

  protected String getPurchaseQuery() {
    return "INSERT INTO sm_transactions (uuid, tran_type, tran_date, symbol, quantity, single_price"
        + ", broker_fee) VALUES (?, 'purchase', ?, ?, ?, ?, ?);" + getLastInsertQuery();
  }

  protected String getSaleQuery() {
    return "INSERT INTO sm_transactions (uuid, tran_type, tran_date, symbol, quantity, single_price"
        + ", broker_fee, earnings) VALUES (?, 'sale', ?, ?, ?, ?, ?, ?);" + getLastInsertQuery();
  }

  protected String getPlayerTransactionsQuery() {
    return SELECT_ALL + " WHERE uuid = ? ORDER BY tran_date";
  }

  protected String getStockTransactionsQuery() {
    return SELECT_ALL + " WHERE symbol = ? ORDER BY tran_date";
  }

  protected String getRecentTransactionsQuery() {
    return SELECT_ALL + " ORDER BY tran_date LIMIT 100";
  }

  protected String getMarkSoldQuery() {
    return "UPDATE sm_transactions SET sold = true WHERE id = ?";
  }
}
