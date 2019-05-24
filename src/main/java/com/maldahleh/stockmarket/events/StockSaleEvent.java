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

  private Player player;
  private String stockSymbol;
  private int quantity;
  private String stockValue;
  private String brokerFees;
  private String grandTotal;
  private String initialPurchase;
  private String netOnTransaction;

  @Override
  public HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }
}
