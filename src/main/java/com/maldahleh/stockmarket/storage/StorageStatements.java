package com.maldahleh.stockmarket.storage;

public abstract class StorageStatements {

  private static final String SELECT_ALL = "SELECT id, uuid, tran_type, tran_date, symbol, "
      + "quantity, single_price, broker_fee, earnings, sold FROM sm_transactions";

  protected String getCreateTableQuery() {
    return null;
  }

  protected String getPurchaseQuery() {
    return "INSERT INTO sm_transactions (id, uuid, tran_type, tran_date, symbol, quantity, "
        + "single_price, broker_fee) VALUES (?, ?, 'purchase', ?, ?, ?, ?, ?)";
  }

  protected String getSaleQuery() {
    return "INSERT INTO sm_transactions (id, uuid, tran_type, tran_date, symbol, quantity, "
        + "single_price, broker_fee, earnings) VALUES (?, ?, 'sale', ?, ?, ?, ?, ?, ?)";
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

  protected String getMaxIdQuery() {
    return "SELECT MAX(id) FROM sm_transactions";
  }
}
