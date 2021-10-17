package com.maldahleh.stockmarket.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public class StockPurchaseEvent extends Event {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  private final Player player;
  private final String stockSymbol;
  private final int quantity;
  private final double stockValue;
  private final double brokerFees;
  private final double grandTotal;

  @Override
  public HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  @SuppressWarnings({"unused", "java:S4144"})
  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }
}
