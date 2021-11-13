package com.maldahleh.stockmarket.players.listeners;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.players.PlayerManager;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerListenerTests {

  private PlayerManager playerManager;
  private PlayerListener playerListener;

  @BeforeEach
  void setup() {
    this.playerManager = mock(PlayerManager.class);
    this.playerListener = new PlayerListener(playerManager);
  }

  @Test
  void playerJoin() {
    // GIVEN
    Player player = mock(Player.class);

    UUID uuid = UUID.randomUUID();
    when(player.getUniqueId())
        .thenReturn(uuid);

    PlayerJoinEvent e = new PlayerJoinEvent(player, null);

    // WHEN
    playerListener.onJoin(e);

    // THEN
    verify(playerManager)
        .cachePlayer(uuid);
  }

  @Test
  void playerQuit() {
    // GIVEN
    Player player = mock(Player.class);

    UUID uuid = UUID.randomUUID();
    when(player.getUniqueId())
        .thenReturn(uuid);

    PlayerQuitEvent e = new PlayerQuitEvent(player, null);

    // WHEN
    playerListener.onQuit(e);

    // THEN
    verify(playerManager)
        .uncachePlayer(uuid);
  }
}
