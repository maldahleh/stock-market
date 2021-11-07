package com.maldahleh.stockmarket.processor;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.processor.model.ProcessorContext;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import yahoofinance.Stock;

@RequiredArgsConstructor
public abstract class StockProcessor {

  protected final StockMarket stockMarket;
  protected final StockManager stockManager;
  protected final PlayerManager playerManager;
  protected final Storage storage;
  protected final Settings settings;
  protected final Messages messages;

  protected abstract boolean shouldBlockStockPlayer(ProcessorContext context);

  protected abstract void calculateTotals(ProcessorContext context);

  protected abstract boolean hasInsufficientFunds(ProcessorContext context);

  protected abstract Transaction buildTransaction(ProcessorContext context);

  protected abstract Event buildEvent(ProcessorContext context);

  protected abstract void processVault(ProcessorContext context);

  protected abstract void sendMessage(ProcessorContext context);

  public void processTransaction(Player player, String symbol, int quantity) {
    ProcessorContext context = new ProcessorContext(player, symbol, quantity);
    StockPlayer stockPlayer = getStockPlayer(context);
    if (stockPlayer == null) {
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(stockMarket, () -> {
      Stock stock = lookupStock(context);
      if (stock == null) {
        messages.sendInvalidStock(context.getPlayer());
        return;
      }

      BigDecimal brokerFees = getBrokerFees(context);
      context.setBrokerFees(brokerFees);

      calculateTotals(context);

      Bukkit.getScheduler().runTask(stockMarket, () -> {
        if (!hasInsufficientFunds(context)) {
          messages.sendInsufficientFunds(player);
          return;
        }

        Transaction transaction = buildTransaction(context);
        context.setTransaction(transaction);

        processVault(context);
        sendMessage(context);
        Bukkit.getPluginManager().callEvent(buildEvent(context));
        playerManager.registerTransaction(context.getPlayer().getUniqueId(), transaction);

        Bukkit.getScheduler().runTaskAsynchronously(stockMarket,
            () -> storage.processTransaction(transaction));
      });
    });
  }

  protected BigDecimal getBrokerFees(ProcessorContext context) {
    if (!(this instanceof PurchaseProcessor) || !settings.getBrokerSettings().isBrokerOnSale()) {
      return BigDecimal.ZERO;
    }

    return context.getQuantityPrice()
            .multiply(settings.getBrokerSettings().getBrokerPercent())
            .add(settings.getBrokerSettings().getBrokerFlat());
  }

  private StockPlayer getStockPlayer(ProcessorContext context) {
    if (playerManager.canNotPerformTransaction(context.getPlayer().getUniqueId())) {
      messages.sendCooldownMessage(context.getPlayer());
      return null;
    }

    StockPlayer stockPlayer = lookupStockPlayer(context);
    if (stockPlayer == null) {
      return null;
    }

    boolean shouldBlockTransaction = shouldBlockStockPlayer(context);
    if (shouldBlockTransaction) {
      messages.sendInvalidSale(context.getPlayer());
      return null;
    }

    return stockPlayer;
  }

  private StockPlayer lookupStockPlayer(ProcessorContext context) {
    StockPlayer stockPlayer = playerManager.getStockPlayer(context.getPlayer().getUniqueId());
    if (stockPlayer == null) {
      return null;
    }

    context.setStockPlayer(stockPlayer);
    return stockPlayer;
  }

  private Stock lookupStock(ProcessorContext context) {
    Stock stock = stockManager.getStock(context.getSymbol());
    if (stockManager.canNotUseStock(context.getPlayer(), stock)) {
      return null;
    }

    BigDecimal price = stockManager.getServerPrice(stock);
    if (price == null) {
      return null;
    }

    context.setStock(stock);
    context.setServerPrice(price);
    return stock;
  }
}
