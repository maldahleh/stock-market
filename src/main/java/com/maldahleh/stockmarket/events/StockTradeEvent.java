package com.maldahleh.stockmarket.events;

import java.math.BigDecimal;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/** Fired after a trade has been saved and paid. Purchases and sales each have a subclass. */
public abstract class StockTradeEvent extends Event {

  private final Player player;
  private final String symbol;
  private final long shares;
  private final BigDecimal price;
  private final BigDecimal fees;
  private final BigDecimal total;

  protected StockTradeEvent(Player player, String symbol, long shares, BigDecimal price,
      BigDecimal fees, BigDecimal total) {
    this.player = player;
    this.symbol = symbol;
    this.shares = shares;
    this.price = price;
    this.fees = fees;
    this.total = total;
  }

  public Player getPlayer() {
    return player;
  }

  public String getSymbol() {
    return symbol;
  }

  public long getShares() {
    return shares;
  }

  /** Server price per share. */
  public BigDecimal getPrice() {
    return price;
  }

  public BigDecimal getFees() {
    return fees;
  }

  /** Money paid for a purchase, or received for a sale. */
  public BigDecimal getTotal() {
    return total;
  }
}
