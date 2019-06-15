package com.maldahleh.stockmarket.players;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.players.listeners.PlayerListener;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.players.player.data.StockData;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import yahoofinance.Stock;

public class PlayerManager {
  private final Map<UUID, Long> lastActionMap;
  private final Map<UUID, StockPlayer> stockPlayerMap;
  private final StockMarket stockMarket;
  private final StockManager stockManager;
  private final Storage storage;
  private final Settings settings;

  public PlayerManager(StockMarket stockMarket, StockManager stockManager, Storage storage,
      Settings settings) {
    this.lastActionMap = new ConcurrentHashMap<>();
    this.stockPlayerMap = new ConcurrentHashMap<>();
    this.stockMarket = stockMarket;
    this.stockManager = stockManager;
    this.storage = storage;
    this.settings = settings;

    Bukkit.getPluginManager().registerEvents(new PlayerListener(this), stockMarket);
  }

  public void cachePlayer(UUID uuid) {
    StockPlayer player = new StockPlayer();
    stockPlayerMap.put(uuid, player);

      Bukkit.getScheduler().runTaskAsynchronously(stockMarket, () -> storage
          .getPlayerTransactions(uuid).forEach(transaction -> {
            if (transaction.getTransactionType().equalsIgnoreCase("purchase")) {
              addPurchaseTransaction(uuid, transaction);
            } else {
              addSaleTransaction(uuid, transaction);
            }
          }));
  }

  public boolean canNotPerformTransaction(UUID uuid) {
    if (settings.getTransactionCooldownSeconds() == 0) {
      return false;
    }

    Long lastAction = lastActionMap.get(uuid);
    if (lastAction == null) {
      return false;
    }

    return ((System.currentTimeMillis() - lastAction) / 1000) <
        settings.getTransactionCooldownSeconds();
  }

  public StockPlayer getStockPlayer(UUID uuid) {
    return stockPlayerMap.get(uuid);
  }

  public StockPlayer forceGetStockPlayer(UUID uuid) {
    StockPlayer cachedPlayer = stockPlayerMap.get(uuid);
    if (cachedPlayer != null) {
      return cachedPlayer;
    }

    StockPlayer stockPlayer = new StockPlayer();
    storage.getPlayerTransactions(uuid).forEach(transaction -> {
      if (transaction.getTransactionType().equalsIgnoreCase("purchase")) {
        addPurchaseTransaction(uuid, transaction);
      } else {
        addSaleTransaction(uuid, transaction);
      }
    });
    return stockPlayer;
  }

  public BigDecimal getCurrentValue(StockPlayer stockPlayer) {
    BigDecimal currentValue = BigDecimal.ZERO;
    for (Map.Entry<String, StockData> e : stockPlayer.getStockMap().entrySet()) {
      Stock stock = stockManager.getStock(e.getKey());
      if (stock == null) {
        continue;
      }

      BigDecimal serverPrice = stockManager.getServerPrice(stock, settings.getPriceMultiplier());
      if (serverPrice == null) {
        continue;
      }

      currentValue = currentValue.add(serverPrice.multiply(BigDecimal.valueOf(e.getValue()
          .getQuantity())));
    }

    return currentValue;
  }

  public BigDecimal getProfitMargin(StockPlayer stockPlayer, BigDecimal currentValue) {
    return currentValue.subtract(stockPlayer.getPortfolioValue());
  }

  public void addPurchaseTransaction(UUID uuid, Transaction transaction) {
    StockPlayer player = stockPlayerMap.get(uuid);
    if (player == null) {
      player = new StockPlayer();
      stockPlayerMap.put(uuid, player);
    }

    lastActionMap.put(uuid, System.currentTimeMillis());
    player.addPurchaseTransaction(transaction);
  }

  public void addSaleTransaction(UUID uuid, Transaction transaction) {
    StockPlayer player = stockPlayerMap.get(uuid);
    if (player == null) {
      player = new StockPlayer();
      stockPlayerMap.put(uuid, player);
    }

    lastActionMap.put(uuid, System.currentTimeMillis());
    player.addSaleTransaction(transaction);
  }

  public void uncachePlayer(UUID uuid) {
    stockPlayerMap.remove(uuid);
    lastActionMap.remove(uuid);
  }
}
