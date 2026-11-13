package com.maldahleh.stockmarket.events;

import java.math.BigDecimal;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class StockSaleEvent extends StockTradeEvent {

  private static final HandlerList HANDLERS = new HandlerList();

  public StockSaleEvent(Player player, String symbol, long shares, BigDecimal price,
      BigDecimal fees, BigDecimal total) {
    super(player, symbol, shares, price, fees, total);
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
