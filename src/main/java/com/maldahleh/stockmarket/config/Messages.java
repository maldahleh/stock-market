package com.maldahleh.stockmarket.config;

import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.utils.Utils;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Messages {
  private final Settings settings;

  private final String lowPriceStock;
  private final String disabledStock;
  private final String invalidStock;
  private final String invalidQuantity;
  private final String insufficientFunds;
  private final String noPermission;
  private final List<String> boughtMessage;

  public Messages(ConfigurationSection section, Settings settings) {
    this.settings = settings;

    this.lowPriceStock = Utils.color(section.getString("low-price-stock"));
    this.disabledStock = Utils.color(section.getString("disabled-stock"));
    this.invalidStock = Utils.color(section.getString("invalid-stock"));
    this.invalidQuantity = Utils.color(section.getString("invalid-quantity"));
    this.insufficientFunds = Utils.color(section.getString("insufficient-funds"));
    this.noPermission = Utils.color(section.getString("no-permission"));
    this.boughtMessage = section.getStringList("bought-stock").stream().map(Utils::color)
        .collect(Collectors.toList());
  }

  public void sendLowPriceStock(Player player) {
    player.sendMessage(lowPriceStock);
  }

  public void sendDisabledStock(Player player) {
    player.sendMessage(disabledStock);
  }

  public void sendInvalidStock(Player player) {
    player.sendMessage(invalidStock);
  }

  public void sendInvalidQuantity(Player player) {
    player.sendMessage(invalidQuantity);
  }

  public void sendInsufficientFunds(Player player) {
    player.sendMessage(insufficientFunds);
  }

  public void sendNoPermission(Player player) {
    player.sendMessage(noPermission);
  }

  public void sendBoughtStockMessage(Player player, String company, Transaction transaction) {
    for (String line : boughtMessage) {
      player.sendMessage(line.replace("<date>", Utils.getCurrentTime())
          .replace("<company>", company).replace("<symbol>", transaction.getSymbol())
          .replace("<quantity>", String.valueOf(transaction.getQuantity()))
          .replace("<stock-value>", Utils.formatCurrency(transaction.getSinglePrice()
              .doubleValue(), settings.getLocale()))
          .replace("<broker-fees>", Utils.formatCurrency(transaction.getBrokerFee()
              .doubleValue(), settings.getLocale()))
          .replace("<total>", Utils.formatCurrency(transaction.getGrandTotal().doubleValue(),
              settings.getLocale())));
    }
  }
}
