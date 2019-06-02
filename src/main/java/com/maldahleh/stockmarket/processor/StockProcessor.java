package com.maldahleh.stockmarket.processor;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.events.StockPurchaseEvent;
import com.maldahleh.stockmarket.events.StockSaleEvent;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import yahoofinance.Stock;

@AllArgsConstructor
public class StockProcessor {
  private final StockMarket stockMarket;
  private final StockManager stockManager;
  private final PlayerManager playerManager;
  private final Storage storage;
  private final Settings settings;
  private final Messages messages;

  public void buyStock(Player player, String symbol, int quantity) {
    Bukkit.getScheduler().runTaskAsynchronously(stockMarket, () -> {
      Stock stock = stockManager.getStock(symbol);
      if (stockManager.canNotUseStock(player, stock, settings, messages)) {
        return;
      }

      if (playerManager.canNotPerformTransaction(player.getUniqueId())) {
        messages.sendCooldownMessage(player);
        return;
      }

      BigDecimal price = stockManager.getServerPrice(stock, settings.getPriceMultiplier());
      if (price == null) {
        messages.sendInvalidStock(player);
        return;
      }

      BigDecimal quantityPrice = price.multiply(BigDecimal.valueOf(quantity));
      BigDecimal brokerFees = quantityPrice.multiply(settings.getBrokerPercentRate())
          .add(settings.getBrokerFlat());
      BigDecimal grandTotal = quantityPrice.add(brokerFees);

      if (!stockMarket.getEcon().has(player, grandTotal.doubleValue())) {
        messages.sendInsufficientFunds(player);
        return;
      }

      stockMarket.getEcon().withdrawPlayer(player, grandTotal.doubleValue());
      Transaction transaction = new Transaction(player.getUniqueId(), "PURCHASE",
          Instant.now(), stock.getSymbol(), quantity, price, brokerFees, null, null,
          grandTotal, false);
      Bukkit.getPluginManager().callEvent(new StockPurchaseEvent(player, symbol, quantity,
          price.doubleValue(), brokerFees.doubleValue(), grandTotal.doubleValue()));
      playerManager.addPurchaseTransaction(player.getUniqueId(), transaction);
      messages.sendBoughtStockMessage(player, stock.getName(), transaction);
      storage.processPurchase(transaction);
    });
  }

  public void sellStock(Player player, String symbol, int quantity) {
    Bukkit.getScheduler().runTaskAsynchronously(stockMarket, () -> {
      StockPlayer stockPlayer = playerManager.getStockPlayer(player.getUniqueId());
      if (stockPlayer == null) {
        return;
      }

      if (playerManager.canNotPerformTransaction(player.getUniqueId())) {
        messages.sendCooldownMessage(player);
        return;
      }

      Collection<Transaction> transactions = stockPlayer.getTransactions();
      if (transactions == null || transactions.isEmpty()) {
        messages.sendInvalidSale(player);
        return;
      }

      Stock stock = stockManager.getStock(symbol);
      if (stockManager.canNotUseStock(player, stock, settings, messages)) {
        return;
      }

      BigDecimal price = stockManager.getServerPrice(stock, settings.getPriceMultiplier());
      if (price == null) {
        messages.sendInvalidStock(player);
        return;
      }

      BigDecimal soldValue = BigDecimal.ZERO;
      int soldQuantity = 0;
      for (Transaction transaction : transactions) {
        if (!transaction.getSymbol().equalsIgnoreCase(symbol)
            || transaction.isSold()) {
          continue;
        }

        if (!transaction.hasElapsed(settings.getMinutesBetweenSale())) {
          continue;
        }

        soldQuantity += transaction.getQuantity();
        soldValue = soldValue.add(transaction.getStockValue());
        transaction.markSold();
        storage.markSold(player.getUniqueId(), transaction);
        if (soldQuantity == quantity) {
          break;
        }
      }

      if (soldQuantity != quantity) {
        messages.sendInvalidSale(player);
        return;
      }

      BigDecimal quantityPrice = price.multiply(BigDecimal.valueOf(quantity));
      BigDecimal brokerFees = BigDecimal.ZERO;
      BigDecimal grandTotal = quantityPrice;
      BigDecimal net = quantityPrice.subtract(soldValue);

      if (settings.isBrokerOnSale()) {
        brokerFees = quantityPrice.multiply(settings.getBrokerPercentRate())
            .add(settings.getBrokerFlat());
        grandTotal = grandTotal.subtract(brokerFees);
      }

      stockMarket.getEcon().depositPlayer(player, grandTotal.doubleValue());
      Transaction transaction = new Transaction(player.getUniqueId(), "SALE",
          Instant.now(), stock.getSymbol(), quantity, price, brokerFees, net, null,
          grandTotal, false);
      Bukkit.getPluginManager().callEvent(new StockSaleEvent(player, symbol, quantity,
          price.doubleValue(), brokerFees.doubleValue(), grandTotal.doubleValue(),
          soldValue.doubleValue(), net.doubleValue()));
      playerManager.addSaleTransaction(player.getUniqueId(), transaction);
      messages.sendSoldStockMessage(player, stock.getName(), transaction);
      storage.processSale(transaction);
    });
  }
}
