package com.maldahleh.stockmarket.players.listeners;

import com.maldahleh.stockmarket.players.PlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public record PlayerListener(PlayerManager playerManager) implements Listener {

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    playerManager.cachePlayer(e.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    playerManager.uncachePlayer(e.getPlayer().getUniqueId());
  }
}
