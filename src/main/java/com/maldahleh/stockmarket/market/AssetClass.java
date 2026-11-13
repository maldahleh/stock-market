package com.maldahleh.stockmarket.market;

public enum AssetClass {
  STOCK,
  CRYPTO;

  /** Crypto symbols are written with a slash, e.g. BTC/USD. */
  public static AssetClass of(String symbol) {
    return symbol.contains("/") ? CRYPTO : STOCK;
  }
}
