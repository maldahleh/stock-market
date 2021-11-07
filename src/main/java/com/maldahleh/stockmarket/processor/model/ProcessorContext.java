package com.maldahleh.stockmarket.processor.model;

import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.util.Collection;
import lombok.Data;
import org.bukkit.entity.Player;

@Data
public class ProcessorContext {

  private final Player player;
  private final String symbol;
  private final int quantity;

  private StockPlayer stockPlayer;
  private Collection<Transaction> processedTransactions;
}
