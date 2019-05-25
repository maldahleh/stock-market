package com.maldahleh.stockmarket.config;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class Settings {
  private final Locale locale;
  private final String unknownData;
  private final BigDecimal minimumPrice;
  private final BigDecimal priceMultiplier;
  private final BigDecimal brokerFlat;
  private final String brokerFlatString;
  private final BigDecimal brokerPercent;
  private final BigDecimal brokerPercentRate;
  private final String brokerPercentString;
  private final Set<String> allowedCurrencies;
  private final Set<String> allowedExchanges;

  public Settings(ConfigurationSection section) {
    this.locale = Locale.forLanguageTag(section.getString("locale"));
    this.unknownData = section.getString("unknown-data");
    this.minimumPrice = BigDecimal.valueOf(section.getDouble("minimum-price"));
    this.priceMultiplier = BigDecimal.valueOf(section.getInt("price-multiplier"));
    this.brokerFlat = BigDecimal.valueOf(section.getDouble("broker-flat"));
    this.brokerFlatString = brokerFlat.toPlainString();
    this.brokerPercentRate = BigDecimal.valueOf(section.getDouble("broker-percent"));
    this.brokerPercent = brokerPercentRate.add(BigDecimal.ONE);
    this.brokerPercentString = brokerPercentRate.multiply(BigDecimal.valueOf(100)).toPlainString();
    this.allowedCurrencies = new HashSet<>(section.getStringList("allowed-currencies"));
    this.allowedExchanges = new HashSet<>(section.getStringList("allowed-exchanges"));
  }

  public boolean isAboveMinimumPrice(BigDecimal price) {
    return price.compareTo(minimumPrice) >= 0;
  }

  public boolean isAllowedCurrency(String symbol) {
    return allowedCurrencies.isEmpty() || allowedCurrencies.contains(symbol.toUpperCase());
  }

  public boolean isAllowedExchange(String exchange) {
    return allowedExchanges.isEmpty() || allowedExchanges.contains(exchange.toUpperCase());
  }
}
