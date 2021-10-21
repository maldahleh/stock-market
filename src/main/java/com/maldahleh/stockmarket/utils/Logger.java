package com.maldahleh.stockmarket.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public class Logger {
  private final String PREFIX = "StockMarket - ";

  public void severe(String message) {
    Bukkit.getLogger().severe(buildLogMessage(message));
  }

  private String buildLogMessage(String message) {
    return String.format("%s %s", PREFIX, message);
  }
}
