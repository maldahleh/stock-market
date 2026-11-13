package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.PluginContext;
import com.maldahleh.stockmarket.storage.Database;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Tells a player about something that happened to their account: in chat if they're online,
 * otherwise saved and shown under "While you were away" when they next join. Call {@link #tell}
 * on the server thread.
 */
public class Notices implements Listener {

  private static final int MAX_ON_JOIN = 10;

  private final Database database;
  private final Messages messages;
  private final PluginContext context;

  public Notices(Database database, Messages messages, PluginContext context) {
    this.database = database;
    this.messages = messages;
    this.context = context;
  }

  /**
   * @param block true for multi-line summaries, sent without the chat prefix
   */
  public void tell(UUID player, String key, Map<String, String> placeholders, boolean block) {
    Player online = Bukkit.getPlayer(player);
    if (online != null) {
      messages.render(key, placeholders, !block).forEach(online::sendMessage);
      return;
    }
    List<String> lines = messages.render(key, placeholders, false);
    context.databaseThread().execute(() -> {
      try {
        database.addNotices(player, lines);
      } catch (SQLException e) {
        context.logger().log(Level.WARNING, "Couldn't save a notice for " + player, e);
      }
    });
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    UUID player = event.getPlayer().getUniqueId();
    CompletableFuture.supplyAsync(() -> {
      try {
        return database.takeNotices(player);
      } catch (SQLException e) {
        throw new CompletionException(e);
      }
    }, context.databaseThread()).whenCompleteAsync((lines, error) -> {
      if (error != null) {
        context.logger().log(Level.WARNING, "Couldn't load notices for " + player, error);
        return;
      }
      Player online = Bukkit.getPlayer(player);
      if (online == null || lines.isEmpty()) {
        return;
      }
      messages.send(online, "notices.header");
      lines.stream().limit(MAX_ON_JOIN).forEach(online::sendMessage);
      if (lines.size() > MAX_ON_JOIN) {
        messages.send(online, "notices.more",
            Map.of("<count>", String.valueOf(lines.size() - MAX_ON_JOIN)));
      }
    }, context.serverThread());
  }
}
