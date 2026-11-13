package com.maldahleh.stockmarket.market;

/** Static information about a listed symbol. */
public record Asset(String symbol, String name, String exchange, boolean tradable) {}
