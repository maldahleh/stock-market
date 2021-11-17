package com.maldahleh.stockmarket.config;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.utils.Utils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.bukkit.entity.Player;

public class Messages {

  private final StockMarket stockMarket;
  private final Settings settings;
  private final ConfigSection configFile;

  public Messages(StockMarket stockMarket, Settings settings) {
    this.stockMarket = stockMarket;
    this.settings = settings;
    this.configFile = new ConfigSection(stockMarket, "messages");
  }

  public void sendCommandsDisabled(Player player) {
    player.sendMessage(configFile.getString("commands-disabled"));
  }

  public void sendLowPriceStock(Player player) {
    player.sendMessage(configFile.getString("low-price-stock"));
  }

  public void sendDisabledStock(Player player) {
    player.sendMessage(configFile.getString("disabled-stock"));
  }

  public void sendCompareMax(Player player) {
    player.sendMessage(configFile.getString("compare-max"));
  }

  public void sendCooldownMessage(Player player) {
    player.sendMessage(configFile.getString("cooldown-message"));
  }

  public void sendInvalidStock(Player player) {
    player.sendMessage(configFile.getString("invalid-stock"));
  }

  public void sendInvalidQuantity(Player player) {
    player.sendMessage(configFile.getString("invalid-quantity"));
  }

  public void sendInvalidSale(Player player) {
    player.sendMessage(configFile.getString("invalid-sale"));
  }

  public void sendInsufficientFunds(Player player) {
    player.sendMessage(configFile.getString("insufficient-funds"));
  }

  public void sendInvalidSyntax(Player player) {
    player.sendMessage(configFile.getString("invalid-syntax"));
  }

  public void sendNoPermission(Player player) {
    player.sendMessage(configFile.getString("no-permission"));
  }

  public void sendNoContent(Player player) {
    player.sendMessage(configFile.getString("no-content"));
  }

  public void sendCitizensRequired(Player player) {
    player.sendMessage(configFile.getString("citizens-required"));
  }

  public void sendPendingLookup(Player player) {
    player.sendMessage(configFile.getString("pending.lookup"));
  }

  public void sendPendingCompare(Player player) {
    player.sendMessage(configFile.getString("pending.compare"));
  }

  public void sendPendingPortfolio(Player player) {
    player.sendMessage(configFile.getString("pending.portfolio"));
  }

  public void sendPendingPortfolioOther(Player player) {
    player.sendMessage(configFile.getString("pending.portfolio-other"));
  }

  public void sendPendingTransactions(Player player) {
    player.sendMessage(configFile.getString("pending.transactions"));
  }

  public void sendPendingTransactionsOther(Player player) {
    player.sendMessage(configFile.getString("pending.transactions-other"));
  }

  public void sendPendingHistory(Player player) {
    player.sendMessage(configFile.getString("pending.history"));
  }

  public void sendPendingHistorySymbol(Player player) {
    player.sendMessage(configFile.getString("pending.history-symbol"));
  }

  public void sendPendingBuy(Player player) {
    player.sendMessage(configFile.getString("pending.buy"));
  }

  public void sendPendingSale(Player player) {
    player.sendMessage(configFile.getString("pending.sale"));
  }

  public void sendBoughtStockMessage(Player player, String company, Transaction transaction) {
    sendTransactionMessage(player, "bought-stock", company, transaction);
  }

  public void sendSoldStockMessage(Player player, String company, Transaction transaction) {
    sendTransactionMessage(player, "sold-stock", company, transaction);
  }

  public void sendHelpMessage(Player player) {
    for (String message : configFile.getStringList("help")) {
      if (message.contains("<commands>")) {
        sendCommandInfo(player);
        continue;
      }

      player.sendMessage(Utils.color(message));
    }
  }

  private void sendTransactionMessage(Player player, String path, String company, Transaction transaction) {
    colorStream(path)
        .map(line -> getFormattedTransactionLine(line, company, transaction))
        .forEach(player::sendMessage);
  }

  private Stream<String> colorStream(String path) {
    return configFile.getStringList(path).stream()
        .map(Utils::color);
  }

  private void sendCommandInfo(Player player) {
    stockMarket.getCommandManager().getRegisteredSubcommands().stream()
        .filter(command -> command.canPlayerExecute(player))
        .map(command -> command.commandHelpKeys(player))
        .flatMap(List::stream)
        .map(path -> configFile.getString("commands." + path))
        .forEach(player::sendMessage);
  }

  private String getFormattedTransactionLine(String line, String company, Transaction transaction) {
    return line.replace("<date>", getCurrentTime())
        .replace("<company>", company)
        .replace("<symbol>", transaction.getSymbol())
        .replace("<quantity>", String.valueOf(transaction.getQuantity()))
        .replace("<stock-value>", settings.format(transaction.getSinglePrice()))
        .replace("<broker-fees>", settings.format(transaction.getBrokerFee()))
        .replace("<total>", settings.format(transaction.getGrandTotal()))
        .replace("<net>", settings.format(transaction.getEarnings()));
  }

  private String getCurrentTime() {
    SimpleDateFormat dateFormat = new SimpleDateFormat();
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    return dateFormat.format(new Date());
  }
}
