package com.maldahleh.stockmarket.config;

import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.utils.Utils;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Messages {

  private final Settings settings;

  private final String commandsDisabled;
  private final String marketClosed;
  private final String lowPriceStock;
  private final String disabledStock;
  private final String compareMax;
  private final String cooldownMessage;
  private final String invalidStock;
  private final String invalidQuantity;
  private final String invalidSale;
  private final String insufficientFunds;
  private final String invalidSyntax;
  private final String noPermission;
  private final List<String> boughtMessage;
  private final List<String> soldMessage;
  private final List<String> helpMessage;

  private final String helpCommandInfo;
  private final String listCommandInfo;
  private final String tutorialCommandInfo;
  private final String lookupCommandInfo;
  private final String compareCommandInfo;
  private final String portfolioCommandInfo;
  private final String portfolioOtherCommandInfo;
  private final String transactionsCommandInfo;
  private final String transactionsOtherCommandInfo;
  private final String historyCommandInfo;
  private final String historySymbolCommandInfo;
  private final String buyCommandInfo;
  private final String sellCommandInfo;
  private final String simpleBrokerCommandInfo;

  private final String pendingLookup;
  private final String pendingCompare;
  private final String pendingPortfolio;
  private final String pendingPortfolioOther;
  private final String pendingTransactions;
  private final String pendingTransactionsOther;
  private final String pendingHistory;
  private final String pendingHistorySymbol;
  private final String pendingBuy;
  private final String pendingSale;

  public Messages(ConfigurationSection section, Settings settings) {
    this.settings = settings;

    this.commandsDisabled = Utils.color(section.getString("commands-disabled"));
    this.marketClosed = Utils.color(section.getString("market-closed"));
    this.lowPriceStock = Utils.color(section.getString("low-price-stock"));
    this.disabledStock = Utils.color(section.getString("disabled-stock"));
    this.compareMax = Utils.color(section.getString("compare-max"));
    this.cooldownMessage = Utils.color(section.getString("cooldown-message"));
    this.invalidStock = Utils.color(section.getString("invalid-stock"));
    this.invalidQuantity = Utils.color(section.getString("invalid-quantity"));
    this.invalidSale = Utils.color(section.getString("invalid-sale"));
    this.insufficientFunds = Utils.color(section.getString("insufficient-funds"));
    this.invalidSyntax = Utils.color(section.getString("invalid-syntax"));
    this.noPermission = Utils.color(section.getString("no-permission"));
    this.boughtMessage =
        section.getStringList("bought-stock").stream()
            .map(Utils::color)
            .collect(Collectors.toList());
    this.soldMessage =
        section.getStringList("sold-stock").stream().map(Utils::color).collect(Collectors.toList());
    this.helpMessage =
        section.getStringList("help").stream().map(Utils::color).collect(Collectors.toList());

    this.helpCommandInfo = Utils.color(section.getString("commands.help"));
    this.listCommandInfo = Utils.color(section.getString("commands.list"));
    this.tutorialCommandInfo = Utils.color(section.getString("commands.tutorial"));
    this.lookupCommandInfo = Utils.color(section.getString("commands.lookup"));
    this.compareCommandInfo = Utils.color(section.getString("commands.compare"));
    this.portfolioCommandInfo = Utils.color(section.getString("commands.portfolio"));
    this.portfolioOtherCommandInfo = Utils.color(section.getString("commands.portfolio-other"));
    this.transactionsCommandInfo = Utils.color(section.getString("commands.transactions"));
    this.transactionsOtherCommandInfo =
        Utils.color(section.getString("commands.transactions-other"));
    this.historyCommandInfo = Utils.color(section.getString("commands.history"));
    this.historySymbolCommandInfo = Utils.color(section.getString("commands.history-symbol"));
    this.buyCommandInfo = Utils.color(section.getString("commands.buy"));
    this.sellCommandInfo = Utils.color(section.getString("commands.sell"));
    this.simpleBrokerCommandInfo = Utils.color(section.getString("commands.simplebroker"));

    this.pendingLookup = Utils.color(section.getString("pending.lookup"));
    this.pendingCompare = Utils.color(section.getString("pending.compare"));
    this.pendingPortfolio = Utils.color(section.getString("pending.portfolio"));
    this.pendingPortfolioOther = Utils.color(section.getString("pending.portfolio-other"));
    this.pendingTransactions = Utils.color(section.getString("pending.transactions"));
    this.pendingTransactionsOther = Utils.color(section.getString("pending.transactions-other"));
    this.pendingHistory = Utils.color(section.getString("pending.history"));
    this.pendingHistorySymbol = Utils.color(section.getString("pending.history-symbol"));
    this.pendingBuy = Utils.color(section.getString("pending.buy"));
    this.pendingSale = Utils.color(section.getString("pending.sale"));
  }

  public void sendCommandsDisabled(Player player) {
    player.sendMessage(commandsDisabled);
  }

  public void sendMarketClosed(Player player) {
    player.sendMessage(marketClosed);
  }

  public void sendLowPriceStock(Player player) {
    player.sendMessage(lowPriceStock);
  }

  public void sendDisabledStock(Player player) {
    player.sendMessage(disabledStock);
  }

  public void sendCompareMax(Player player) {
    player.sendMessage(compareMax);
  }

  public void sendCooldownMessage(Player player) {
    player.sendMessage(cooldownMessage);
  }

  public void sendInvalidStock(Player player) {
    player.sendMessage(invalidStock);
  }

  public void sendInvalidQuantity(Player player) {
    player.sendMessage(invalidQuantity);
  }

  public void sendInvalidSale(Player player) {
    player.sendMessage(invalidSale);
  }

  public void sendInsufficientFunds(Player player) {
    player.sendMessage(insufficientFunds);
  }

  public void sendInvalidSyntax(Player player) {
    player.sendMessage(invalidSyntax);
  }

  public void sendNoPermission(Player player) {
    player.sendMessage(noPermission);
  }

  public void sendBoughtStockMessage(Player player, String company, Transaction transaction) {
    for (String line : boughtMessage) {
      player.sendMessage(
          line.replace("<date>", Utils.getCurrentTime())
              .replace("<company>", company)
              .replace("<symbol>", transaction.getSymbol())
              .replace("<quantity>", String.valueOf(transaction.getQuantity()))
              .replace(
                  "<stock-value>",
                  Utils.formatCurrency(
                      transaction.getSinglePrice().doubleValue(), settings.getLocale()))
              .replace(
                  "<broker-fees>",
                  Utils.formatCurrency(
                      transaction.getBrokerFee().doubleValue(), settings.getLocale()))
              .replace(
                  "<total>",
                  Utils.formatCurrency(
                      transaction.getGrandTotal().doubleValue(), settings.getLocale())));
    }
  }

  public void sendSoldStockMessage(Player player, String company, Transaction transaction) {
    for (String line : soldMessage) {
      player.sendMessage(
          line.replace("<date>", Utils.getCurrentTime())
              .replace("<company>", company)
              .replace("<symbol>", transaction.getSymbol())
              .replace("<quantity>", String.valueOf(transaction.getQuantity()))
              .replace(
                  "<stock-value>",
                  Utils.formatCurrency(
                      transaction.getSinglePrice().doubleValue(), settings.getLocale()))
              .replace(
                  "<broker-fees>",
                  Utils.formatCurrency(
                      transaction.getBrokerFee().doubleValue(), settings.getLocale()))
              .replace(
                  "<total>",
                  Utils.formatCurrency(
                      transaction.getGrandTotal().doubleValue(), settings.getLocale()))
              .replace(
                  "<net>",
                  Utils.formatCurrency(
                      transaction.getEarnings().doubleValue(), settings.getLocale())));
    }
  }

  public void sendHelpMessage(Player player) {
    for (String message : helpMessage) {
      if (message.contains("<commands>")) {
        sendCommandInfo(player);
        continue;
      }

      player.sendMessage(message);
    }
  }

  private void sendCommandInfo(Player player) {
    if (!player.hasPermission("stockmarket.use")) {
      return;
    }

    player.sendMessage(helpCommandInfo);
    if (player.hasPermission("stockmarket.tutorial")) {
      player.sendMessage(tutorialCommandInfo);
    }

    if (player.hasPermission("stockmarket.list")) {
      player.sendMessage(listCommandInfo);
    }

    if (player.hasPermission("stockmarket.lookup")) {
      player.sendMessage(lookupCommandInfo);
    }

    if (player.hasPermission("stockmarket.compare")) {
      player.sendMessage(compareCommandInfo);
    }

    if (player.hasPermission("stockmarket.portfolio")) {
      player.sendMessage(portfolioCommandInfo);
    }

    if (player.hasPermission("stockmarket.portfolio.other")) {
      player.sendMessage(portfolioOtherCommandInfo);
    }

    if (player.hasPermission("stockmarket.transactions")) {
      player.sendMessage(transactionsCommandInfo);
    }

    if (player.hasPermission("stockmarket.transactions.other")) {
      player.sendMessage(transactionsOtherCommandInfo);
    }

    if (player.hasPermission("stockmarket.history")) {
      player.sendMessage(historyCommandInfo);
      player.sendMessage(historySymbolCommandInfo);
    }

    player.sendMessage(buyCommandInfo);
    player.sendMessage(sellCommandInfo);

    if (player.hasPermission("stockmarket.spawnbroker")) {
      player.sendMessage(simpleBrokerCommandInfo);
    }
  }

  public void sendPendingLookup(Player player) {
    player.sendMessage(pendingLookup);
  }

  public void sendPendingCompare(Player player) {
    player.sendMessage(pendingCompare);
  }

  public void sendPendingPortfolio(Player player) {
    player.sendMessage(pendingPortfolio);
  }

  public void sendPendingPortfolioOther(Player player) {
    player.sendMessage(pendingPortfolioOther);
  }

  public void sendPendingTransactions(Player player) {
    player.sendMessage(pendingTransactions);
  }

  public void sendPendingTransactionsOther(Player player) {
    player.sendMessage(pendingTransactionsOther);
  }

  public void sendPendingHistory(Player player) {
    player.sendMessage(pendingHistory);
  }

  public void sendPendingHistorySymbol(Player player) {
    player.sendMessage(pendingHistorySymbol);
  }

  public void sendPendingBuy(Player player) {
    player.sendMessage(pendingBuy);
  }

  public void sendPendingSale(Player player) {
    player.sendMessage(pendingSale);
  }
}
