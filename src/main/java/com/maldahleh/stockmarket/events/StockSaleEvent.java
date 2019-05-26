package com.maldahleh.stockmarket.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public class StockSaleEvent extends Event {
  private static HandlerList HANDLER_LIST = new HandlerList();

  private final Player player;
  private final String stockSymbol;
  private final int quantity;
  private final double stockValue;
  private final double brokerFees;
  private final double grandTotal;
  private final double initialPurchase;
  private final double netOnTransaction;

  @Override
  public HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }
}
