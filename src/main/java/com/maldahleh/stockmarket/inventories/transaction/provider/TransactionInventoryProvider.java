package com.maldahleh.stockmarket.inventories.transaction.provider;

import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.utils.paged.provider.IContentProvider;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.utils.Utils;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
public class TransactionInventoryProvider implements IContentProvider<Instant, Transaction,
    Instant> {
  private final StockMarket stockMarket;
  private final PlayerManager playerManager;
  private final Settings settings;

  @Override
  public Map<Instant, Transaction> getContent(UUID uuid) {
    StockPlayer stockPlayer = playerManager.forceGetStockPlayer(uuid);
    if (stockPlayer == null) {
      return null;
    }

    return stockPlayer.getTransactionMap();
  }

  @Override
  public Map<Instant, Transaction> applyTransformations(Map<Instant, Transaction> data) {
    return data;
  }

  @Override
  public Map<String, Object> getExtraData(UUID uuid) {
    return new HashMap<>();
  }

  @Override
  public ItemStack getContentStack(ItemStack baseStack, int position, Instant key,
      Transaction value) {
    return Utils.updateItemStack(baseStack.clone(), ImmutableMap.<String, Object>builder()
        .put("<date>", Utils.formatInstant(value.getTransactionDate(), settings.getLocale()))
        .put("<symbol>", value.getSymbol().toUpperCase())
        .put("<transaction-type>", value.getTransactionType())
        .put("<quantity>", value.getQuantity())
        .put("<stock-value>", Utils.formatCurrency(value.getStockValue().doubleValue(),
            settings.getLocale()))
        .put("<broker-fees>", Utils.formatCurrency(value.getBrokerFee().doubleValue(), settings
            .getLocale()))
        .put("<grand-total>", Utils.formatCurrency(value.getGrandTotal().doubleValue(),
            settings.getLocale()))
        .put("<earnings>", Utils.format(value.getEarnings(), settings.getUnknownData(),
            settings.getLocale()))
        .put("<server-currency>", stockMarket.getEcon().currencyNamePlural())
        .put("<sold>", String.valueOf(value.isSold()))
        .build());
  }

  @Override
  public ItemStack getExtraItem(ItemStack baseStack, Map<String, Object> extraData) {
    return baseStack;
  }
}
