package com.maldahleh.stockmarket.processor;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.events.StockPurchaseEvent;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.storage.Storage;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import yahoofinance.Stock;

@AllArgsConstructor
public class StockProcessor {
  private final StockMarket stockMarket;
  private final StockManager stockManager;
  private final Storage storage;
  private final Settings settings;
  private final Messages messages;

  public void buyStock(Player player, String symbol, int quantity) {
    Bukkit.getScheduler().runTaskAsynchronously(stockMarket, () -> {
      Stock stock = stockManager.getStock(symbol);
      if (stockManager.canNotUseStock(player, stock, settings, messages)) {
        return;
      }

      BigDecimal price = stockManager.getServerPrice(stock, settings.getPriceMultiplier());
      if (price == null) {
        messages.sendInvalidStock(player);
        return;
      }

      BigDecimal brokerFees = price.multiply(settings.getBrokerPercent())
          .add(settings.getBrokerFlat());
      BigDecimal grandTotal = price.multiply(BigDecimal.valueOf(quantity)).add(brokerFees);

      if (!stockMarket.getEcon().withdrawPlayer(player, grandTotal.doubleValue())
          .transactionSuccess()) {
        messages.sendInsufficientFunds(player);
        return;
      }

      messages.sendBoughtStockMessage(player, stock.getName(), stock.getSymbol(), quantity, price,
          brokerFees, grandTotal);
      Bukkit.getPluginManager().callEvent(new StockPurchaseEvent(player, symbol, quantity,
          price.doubleValue(), brokerFees.doubleValue(), grandTotal.doubleValue()));
      storage.processPurchase(player.getUniqueId(), symbol, quantity, price.doubleValue(),
          brokerFees.doubleValue());
    });
  }
}
