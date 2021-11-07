package com.maldahleh.stockmarket.processor.model;

import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.math.BigDecimal;
import java.util.Collection;
import lombok.Data;
import org.bukkit.entity.Player;
import yahoofinance.Stock;

@Data
public class ProcessorContext {

  private final Player player;
  private final String symbol;
  private final int quantity;

  private Stock stock;
  private BigDecimal serverPrice;
  private StockPlayer stockPlayer;
  private Collection<Transaction> processedTransactions;

  private BigDecimal quantityPrice;
  private BigDecimal brokerFees = BigDecimal.ZERO;
  private BigDecimal grandTotal;
  private BigDecimal soldValue;
  private BigDecimal net;

  private Transaction transaction;

  public void setServerPrice(BigDecimal serverPrice) {
    this.serverPrice = serverPrice;
    this.quantityPrice = serverPrice.multiply(BigDecimal.valueOf(quantity));
  }
}
