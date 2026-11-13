package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.market.MarketData;
import java.util.Map;
import org.bukkit.Bukkit;

/**
 * Announces when the US stock market opens and closes. Run {@link #check} on a timer; it uses
 * the cached market clock, so it costs at most one request a minute. Blank messages turn it off.
 */
public class MarketNews {

  private final MarketData market;
  private final Messages messages;
  private final java.util.concurrent.Executor serverThread;
  private Boolean wasOpen;

  public MarketNews(MarketData market, Messages messages,
      java.util.concurrent.Executor serverThread) {
    this.market = market;
    this.messages = messages;
    this.serverThread = serverThread;
  }

  /** Call on the server thread. */
  public void check() {
    if (messages.raw("market-news.opened").isBlank()
        && messages.raw("market-news.closed").isBlank()) {
      return;
    }
    market.clock().thenAcceptAsync(clock -> announce(clock.open()), serverThread)
        .exceptionally(error -> null);
  }

  private void announce(boolean open) {
    Boolean previous = wasOpen;
    wasOpen = open;
    if (previous == null || previous == open) {
      return;
    }
    String key = open ? "market-news.opened" : "market-news.closed";
    if (!messages.raw(key).isBlank()) {
      messages.render(key, Map.of(), true).forEach(Bukkit::broadcastMessage);
    }
  }
}
