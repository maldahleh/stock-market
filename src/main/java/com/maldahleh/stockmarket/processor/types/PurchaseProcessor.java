package com.maldahleh.stockmarket.processor.types;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.processor.StockProcessor;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.storage.Storage;

public class PurchaseProcessor extends StockProcessor {

  public PurchaseProcessor(StockMarket stockMarket,
      StockManager stockManager,
      PlayerManager playerManager,
      Storage storage,
      Settings settings,
      Messages messages) {
    super(stockMarket, stockManager, playerManager, storage, settings, messages);
  }

  @Override
  protected boolean shouldBlockStockPlayer(StockPlayer stockPlayer, String symbol, int quantity) {
    return false;
  }
}
