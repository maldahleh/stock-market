package com.maldahleh.stockmarket.config;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.commands.CommandManager;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.utils.CurrencyUtils;
import com.maldahleh.stockmarket.utils.TimeUtils;
import com.maldahleh.stockmarket.utils.Utils;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public record Messages(StockMarket stockMarket,
                       ConfigurationSection section,
                       Settings settings) {

  public void sendCommandsDisabled(Player player) {
    player.sendMessage(Utils.color(section.getString("commands-disabled")));
  }

  public void sendMarketClosed(Player player) {
    player.sendMessage(Utils.color(section.getString("market-closed")));
  }

  public void sendLowPriceStock(Player player) {
    player.sendMessage(Utils.color(section.getString("low-price-stock")));
  }

  public void sendDisabledStock(Player player) {
    player.sendMessage(Utils.color(section.getString("disabled-stock")));
  }

  public void sendCompareMax(Player player) {
    player.sendMessage(Utils.color(section.getString("compare-max")));
  }

  public void sendCooldownMessage(Player player) {
    player.sendMessage(Utils.color(section.getString("cooldown-message")));
  }

  public void sendInvalidStock(Player player) {
    player.sendMessage(Utils.color(section.getString("invalid-stock")));
  }

  public void sendInvalidQuantity(Player player) {
    player.sendMessage(Utils.color(section.getString("invalid-quantity")));
  }

  public void sendInvalidSale(Player player) {
    player.sendMessage(Utils.color(section.getString("invalid-sale")));
  }

  public void sendInsufficientFunds(Player player) {
    player.sendMessage(Utils.color(section.getString("insufficient-funds")));
  }

  public void sendInvalidSyntax(Player player) {
    player.sendMessage(Utils.color(section.getString("invalid-syntax")));
  }

  public void sendNoPermission(Player player) {
    player.sendMessage(Utils.color(section.getString("no-permission")));
  }

  public void sendPendingLookup(Player player) {
    player.sendMessage(Utils.color(section.getString("pending.lookup")));
  }

  public void sendPendingCompare(Player player) {
    player.sendMessage(Utils.color(section.getString("pending.compare")));
  }

  public void sendPendingPortfolio(Player player) {
    player.sendMessage(Utils.color(section.getString("pending.portfolio")));
  }

  public void sendPendingPortfolioOther(Player player) {
    player.sendMessage(Utils.color(section.getString("pending.portfolio-other")));
  }

  public void sendPendingTransactions(Player player) {
    player.sendMessage(Utils.color(section.getString("pending.transactions")));
  }

  public void sendPendingTransactionsOther(Player player) {
    player.sendMessage(Utils.color(section.getString("pending.transactions-other")));
  }

  public void sendPendingHistory(Player player) {
    player.sendMessage(Utils.color(section.getString("pending.history")));
  }

  public void sendPendingHistorySymbol(Player player) {
    player.sendMessage(Utils.color(section.getString("pending.history-symbol")));
  }

  public void sendPendingBuy(Player player) {
    player.sendMessage(Utils.color(section.getString("pending.buy")));
  }

  public void sendPendingSale(Player player) {
    player.sendMessage(Utils.color(section.getString("pending.sale")));
  }

  public void sendBoughtStockMessage(Player player, String company, Transaction transaction) {
    section.getStringList("bought-stock").stream()
        .map(Utils::color)
        .map(line -> getFormattedTransactionLine(line, company, transaction))
        .forEach(player::sendMessage);
  }

  public void sendSoldStockMessage(Player player, String company, Transaction transaction) {
    section.getStringList("sold-stock").stream()
        .map(Utils::color)
        .map(line -> getFormattedTransactionLine(line, company, transaction)
            .replace("<net>",
                CurrencyUtils.formatCurrency(
                    transaction.getEarnings().doubleValue(), settings.getLocale())))
        .forEach(player::sendMessage);
  }

  public void sendHelpMessage(Player player) {
    for (String message : section.getStringList("help")) {
      if (message.contains("<commands>")) {
        sendCommandInfo(player);
        continue;
      }

      player.sendMessage(Utils.color(message));
    }
  }

  private void sendCommandInfo(Player player) {
    if (!CommandManager.hasBaseCommandPermission(player)) {
      return;
    }

    stockMarket.getCommandManager().getRegisteredSubcommands().stream()
        .filter(command -> player.hasPermission(command.requiredPerm()))
        .map(command -> command.commandHelpKeys(player))
        .flatMap(List::stream)
        .map(path -> Utils.color(section.getString("commands." + path)))
        .forEach(player::sendMessage);
  }

  private String getFormattedTransactionLine(String line, String company, Transaction transaction) {
    return line.replace("<date>", TimeUtils.getCurrentTime())
        .replace("<company>", company)
        .replace("<symbol>", transaction.getSymbol())
        .replace("<quantity>", String.valueOf(transaction.getQuantity()))
        .replace(
            "<stock-value>",
            CurrencyUtils.formatCurrency(
                transaction.getSinglePrice().doubleValue(), settings.getLocale()))
        .replace(
            "<broker-fees>",
            CurrencyUtils.formatCurrency(
                transaction.getBrokerFee().doubleValue(), settings.getLocale()))
        .replace(
            "<total>",
            CurrencyUtils.formatCurrency(
                transaction.getGrandTotal().doubleValue(), settings.getLocale()));
  }
}
