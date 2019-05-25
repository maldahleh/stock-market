package com.maldahleh.stockmarket.players;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

@RequiredArgsConstructor
public class PlayerManager {
  private final Map<UUID, StockPlayer> stockPlayerMap = new ConcurrentHashMap<>();

  private final StockMarket stockMarket;
  private final Storage storage;

  public void cachePlayer(UUID uuid) {
    StockPlayer player = new StockPlayer();
    stockPlayerMap.put(uuid, player);

    Bukkit.getScheduler().runTaskAsynchronously(stockMarket,
        () -> storage.getPlayerTransactions(uuid).forEach(player::addTransaction));
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
