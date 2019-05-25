package com.maldahleh.stockmarket.players;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.players.listeners.PlayerListener;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;

public class PlayerManager {
  private final Map<UUID, StockPlayer> stockPlayerMap;
  private final StockMarket stockMarket;
  private final Storage storage;

  public PlayerManager(StockMarket stockMarket, Storage storage) {
    this.stockPlayerMap = new ConcurrentHashMap<>();
    this.stockMarket = stockMarket;
    this.storage = storage;

    Bukkit.getPluginManager().registerEvents(new PlayerListener(this), stockMarket);
  }

  public void cachePlayer(UUID uuid) {
    StockPlayer player = new StockPlayer();
    stockPlayerMap.put(uuid, player);

      Bukkit.getScheduler().runTaskAsynchronously(stockMarket,
          () -> storage.getPlayerTransactions(uuid).forEach(player::addTransaction));
  }

  public StockPlayer forceGetStockPlayer(UUID uuid) {
    StockPlayer cachedPlayer = stockPlayerMap.get(uuid);
    if (cachedPlayer != null) {
      return cachedPlayer;
    }

    StockPlayer stockPlayer = new StockPlayer();
    storage.getPlayerTransactions(uuid).forEach(stockPlayer::addTransaction);
    return stockPlayer;
  }

  public void addTransaction(UUID uuid, Transaction transaction) {
    StockPlayer player = stockPlayerMap.get(uuid);
    if (player == null) {
      player = new StockPlayer();
      stockPlayerMap.put(uuid, player);
    }

    player.addTransaction(transaction);
  }

  public void uncachePlayer(UUID uuid) {
    stockPlayerMap.remove(uuid);
  }
}
