package com.maldahleh.stockmarket.config;

import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.config.models.BrokerSettings;
import com.maldahleh.stockmarket.config.models.SqlSettings;
import java.math.BigDecimal;
import java.util.Locale;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class Settings {

  private final ConfigSection configFile;
  @Getter
  private final BrokerSettings brokerSettings;
  @Getter
  private final SqlSettings sqlSettings;

  public Settings(JavaPlugin javaPlugin) {
    this.configFile = new ConfigSection(javaPlugin, "config");
    this.brokerSettings = new BrokerSettings(configFile.getConfigSection("broker"));
    this.sqlSettings = new SqlSettings(configFile.getConfigSection("sql"));
  }

  public Locale getLocale() {
    return configFile.getLocale("locale");
  }

  public int getCacheMinutes() {
    return configFile.getInt("cache.expire-minutes");
  }

  public String getUnknownData() {
    return configFile.getString("unknown-data");
  }

  public int getTransactionCooldownSeconds() {
    return configFile.getInt("transaction-cooldown-seconds");
  }

  public int getMinutesBetweenSale() {
    return configFile.getInt("minutes-between-sale");
  }

  public BigDecimal getPriceMultiplier() {
    return configFile.getBigDecimal("price-multiplier");
  }

  public boolean isAboveMinimumPrice(BigDecimal price) {
    return price.compareTo(configFile.getBigDecimal("minimum-price")) >= 0;
  }

  public boolean isAllowedCurrency(String symbol) {
    return configFile.getStringSet("allowed-currencies").stream()
        .anyMatch(allowedCurrency -> allowedCurrency.equalsIgnoreCase(symbol));
  }

  public boolean isAllowedExchange(String exchange) {
    return configFile.getStringSet("allowed-exchanges").stream()
        .anyMatch(allowedExchange -> allowedExchange.equalsIgnoreCase(exchange));
  }
}
