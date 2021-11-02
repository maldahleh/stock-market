package com.maldahleh.stockmarket.config;

import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.config.models.SqlSettings;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class Settings {

  private final Locale locale;
  private final SqlSettings sqlSettings;

  private final boolean brokerOnSale;
  private final BigDecimal brokerFlat;
  private final BigDecimal brokerPercentRate;

  private final String unknownData;
  private final boolean blockTransactionsWhenClosed;
  private final int transactionCooldownSeconds;
  private final int minutesBetweenSale;
  private final BigDecimal minimumPrice;
  private final BigDecimal priceMultiplier;
  private final Set<String> allowedCurrencies;
  private final Set<String> allowedExchanges;

  public Settings(JavaPlugin javaPlugin) {
    ConfigSection configFile = new ConfigSection(javaPlugin);

    this.locale = configFile.getLocale("locale");
    this.sqlSettings = new SqlSettings(configFile.getConfigSection("storage.mysql"));

    this.brokerOnSale = configFile.getBoolean("broker.charge-fees-on-sale");
    this.brokerFlat = configFile.getBigDecimal("broker.flat-fee");
    this.brokerPercentRate = configFile.getBigDecimal("broker.percent-fee");

    this.unknownData = configFile.getString("unknown-data");
    this.blockTransactionsWhenClosed = configFile.getBoolean("block-transactions-when-market-closed");
    this.transactionCooldownSeconds = configFile.getInt("transaction-cooldown-seconds");
    this.minutesBetweenSale = configFile.getInt("minutes-between-sale");
    this.minimumPrice = configFile.getBigDecimal("minimum-price");
    this.priceMultiplier = configFile.getBigDecimal("price-multiplier");
    this.allowedCurrencies = configFile.getStringSet("allowed-currencies");
    this.allowedExchanges = configFile.getStringSet("allowed-exchanges");
  }

  public String getBrokerPercentString() {
    return brokerPercentRate.multiply(BigDecimal.valueOf(100)).toPlainString();
  }

  public String getBrokerFlatString() {
    return brokerFlat.toPlainString();
  }

  public boolean isAboveMinimumPrice(BigDecimal price) {
    return price.compareTo(minimumPrice) >= 0;
  }

  public boolean isAllowedCurrency(String symbol) {
    return allowedCurrencies.contains(symbol.toUpperCase());
  }

  public boolean isAllowedExchange(String exchange) {
    return allowedExchanges.contains(exchange.toUpperCase());
  }
}
