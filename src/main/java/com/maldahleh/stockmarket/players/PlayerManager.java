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

public class PlayerManager {

  private final Map<UUID, Long> lastActionMap = new ConcurrentHashMap<>();
  private final Map<UUID, StockPlayer> stockPlayerMap = new ConcurrentHashMap<>();

  private final StockMarket stockMarket;
  private final StockManager stockManager;
  private final Storage storage;
  private final Settings settings;

  public PlayerManager(StockMarket stockMarket, StockManager stockManager, Storage storage,
      Settings settings) {
    this.stockMarket = stockMarket;
    this.stockManager = stockManager;
    this.storage = storage;
    this.settings = settings;

    Bukkit.getPluginManager().registerEvents(new PlayerListener(this), stockMarket);
  }

  public void cachePlayer(UUID uuid) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(stockMarket, () -> loadPlayerTransactions(uuid));
  }

  public StockPlayer getStockPlayer(UUID uuid) {
    return stockPlayerMap.get(uuid);
  }

  public StockPlayer forceGetStockPlayer(UUID uuid) {
    StockPlayer cachedPlayer = stockPlayerMap.get(uuid);
    if (cachedPlayer != null) {
      return cachedPlayer;
    }

    storage
        .getPlayerTransactions(uuid)
        .forEach(transaction -> registerTransaction(uuid, transaction));
    return getAndRemoveStockPlayer(uuid);
  }

  public BigDecimal getCurrentValue(StockPlayer stockPlayer) {
    BigDecimal currentValue = BigDecimal.ZERO;
    for (Map.Entry<String, StockData> e : stockPlayer.getStockMap().entrySet()) {
      BigDecimal serverPrice = stockManager.getServerPrice(e.getKey());
      BigDecimal quantity = BigDecimal.valueOf(e.getValue().getQuantity());
      BigDecimal totalValue = serverPrice.multiply(quantity);

      currentValue = currentValue.add(totalValue);
    }

    return currentValue;
  }

  public BigDecimal getProfitMargin(StockPlayer stockPlayer, BigDecimal currentValue) {
    return currentValue.subtract(stockPlayer.getPortfolioValue());
  }

  public void registerTransaction(UUID uuid, Transaction transaction) {
    StockPlayer player = getOrCreateStockPlayer(uuid);
    if (transaction.getTransactionType().equalsIgnoreCase("purchase")) {
      player.addPurchaseTransaction(transaction);
    } else {
      player.addSaleTransaction(transaction);
    }

    lastActionMap.put(uuid, System.currentTimeMillis());
  }

  public void uncachePlayer(UUID uuid) {
    stockPlayerMap.remove(uuid);
    lastActionMap.remove(uuid);
  }

  public boolean canNotPerformTransaction(UUID uuid) {
    if (settings.getTransactionCooldownSeconds() == 0) {
      return false;
    }

    Long lastAction = lastActionMap.get(uuid);
    if (lastAction == null) {
      return false;
    }

    return ((System.currentTimeMillis() - lastAction) / 1000)
        < settings.getTransactionCooldownSeconds();
  }

  private StockPlayer getAndRemoveStockPlayer(UUID uuid)
  {
    StockPlayer player = getStockPlayer(uuid);
    uncachePlayer(uuid);

    return player;
  }

  private void loadPlayerTransactions(UUID uuid) {
    storage
        .getPlayerTransactions(uuid)
        .forEach(transaction -> registerTransaction(uuid, transaction));
  }

  private StockPlayer getOrCreateStockPlayer(UUID uuid) {
    StockPlayer player = stockPlayerMap.get(uuid);
    if (player == null) {
      player = new StockPlayer();
      stockPlayerMap.put(uuid, player);
    }

    return player;
  }
}
