package com.maldahleh.stockmarket.events;

import java.math.BigDecimal;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public class StockSaleEvent extends StockPurchaseEvent {

  private final BigDecimal initialPurchase;
  private final BigDecimal netOnTransaction;

  @SuppressWarnings("java:S107") // too many constructor args
  public StockSaleEvent(
      Player player,
      String stockSymbol,
      int quantity,
      BigDecimal stockValue,
      BigDecimal brokerFees,
      BigDecimal grandTotal,
      BigDecimal initialPurchase,
      BigDecimal netOnTransaction) {
    super(player, stockSymbol, quantity, stockValue, brokerFees, grandTotal);

    this.initialPurchase = initialPurchase;
    this.netOnTransaction = netOnTransaction;
  }
}
