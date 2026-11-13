package com.maldahleh.stockmarket.market;

public class MarketDataException extends Exception {

  private final int status;

  public MarketDataException(String message, int status) {
    super(message);
    this.status = status;
  }

  public MarketDataException(String message, Throwable cause) {
    super(message, cause);
    this.status = -1;
  }

  /** HTTP status, or -1 for network and parse failures. */
  public int status() {
    return status;
  }
}
