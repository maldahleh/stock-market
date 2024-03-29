package com.maldahleh.stockmarket.events;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NonNull;
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
  private final BigDecimal stockValue;
  private final BigDecimal brokerFees;
  private final BigDecimal grandTotal;

  @NonNull
  @Override
  public HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  @SuppressWarnings({"unused", "java:S4144"})
  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }
}
