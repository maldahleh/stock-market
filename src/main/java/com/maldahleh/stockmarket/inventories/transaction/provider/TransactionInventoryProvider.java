package com.maldahleh.stockmarket.inventories.transaction.provider;

import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.utils.paged.provider.ContentProvider;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.utils.Utils;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class TransactionInventoryProvider extends ContentProvider<UUID, Instant, Transaction> {

  private final PlayerManager playerManager;

  public TransactionInventoryProvider(StockMarket stockMarket, PlayerManager playerManager,
      Settings settings) {
    super(stockMarket, settings);

    this.playerManager = playerManager;
  }

  @Override
  public Map<Instant, Transaction> getContent(UUID uuid) {
    Map<Instant, Transaction> data = getData(uuid);
    Map<Instant, Transaction> stockDataMap = new TreeMap<>(Collections.reverseOrder());
    stockDataMap.putAll(data);

    return stockDataMap;
  }

  @Override
  public ItemStack getContentStack(ItemStack baseStack, Instant key, Transaction value) {
    return Utils.updateItemStack(
        baseStack.clone(),
        ImmutableMap.<String, Object>builder()
            .put("<date>", formatInstant(value.getTransactionDate()))
            .put("<symbol>", value.getSymbol().toUpperCase())
            .put("<transaction-type>", value.getTransactionType())
            .put("<quantity>", value.getQuantity())
            .put("<stock-value>", settings.format(value.getStockValue()))
            .put("<broker-fees>", settings.format(value.getBrokerFee()))
            .put("<grand-total>", settings.format(value.getGrandTotal()))
            .put("<earnings>", settings.format(value.getEarnings()))
            .put("<server-currency>", stockMarket.getEcon().currencyNamePlural())
            .put("<sold>", String.valueOf(value.isSold()))
            .build());
  }

  private Map<Instant, Transaction> getData(UUID uuid) {
    StockPlayer stockPlayer = playerManager.forceGetStockPlayer(uuid);
    if (stockPlayer == null) {
      return Collections.emptyMap();
    }

    return stockPlayer.getTransactionMap();
  }
}
