package com.maldahleh.stockmarket.inventories.history.providers;

import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.utils.paged.provider.IContentProvider;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.utils.CurrencyUtils;
import com.maldahleh.stockmarket.utils.TimeUtils;
import com.maldahleh.stockmarket.utils.Utils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

public record StockHistoryProvider(StockMarket stockMarket, Storage storage, Settings settings)
    implements IContentProvider<String, Transaction, UUID, Transaction, OfflinePlayer> {

  @Override
  public Map<Transaction, UUID> getContent(String lookup) {
    if (lookup == null) {
      return storage.getTransactionHistory().stream()
          .collect(Collectors.toMap(t -> t, Transaction::getUuid));
    }

    return storage.getStockTransactions(lookup.toUpperCase()).stream()
        .collect(Collectors.toMap(t -> t, Transaction::getUuid));
  }

  @Override
  public Map<Transaction, OfflinePlayer> applyTransformations(Map<Transaction, UUID> data) {
    Map<Transaction, OfflinePlayer> stockDataMap = new TreeMap<>(new TransactionComparator());
    for (Map.Entry<Transaction, UUID> e : data.entrySet()) {
      if (e.getKey().getQuantity() == 0) {
        continue;
      }

      stockDataMap.put(e.getKey(), Bukkit.getOfflinePlayer(e.getValue()));
    }

    return stockDataMap;
  }

  @Override
  public Map<String, Object> getExtraData(String uuid) {
    return new HashMap<>();
  }

  @Override
  public ItemStack getContentStack(
      ItemStack baseStack, int position, Transaction key, OfflinePlayer value) {
    return Utils.updateItemStack(
        baseStack.clone(),
        ImmutableMap.<String, Object>builder()
            .put("<name>", value.getName())
            .put("<date>", TimeUtils.formatInstant(key.getTransactionDate(), settings.getLocale()))
            .put("<symbol>", key.getSymbol().toUpperCase())
            .put("<transaction-type>", key.getTransactionType())
            .put("<quantity>", key.getQuantity())
            .put("<stock-value>", CurrencyUtils.formatCurrency(key.getStockValue(), settings))
            .put("<broker-fees>", CurrencyUtils.formatCurrency(key.getBrokerFee(), settings))
            .put("<grand-total>", CurrencyUtils.formatCurrency(key.getGrandTotal(), settings))
            .put("<earnings>", CurrencyUtils.format(key.getEarnings(), settings))
            .put("<server-currency>", stockMarket.getEcon().currencyNamePlural())
            .put("<sold>", String.valueOf(key.isSold()))
            .build()
    );
  }

  @Override
  public ItemStack getExtraItem(ItemStack baseStack, Map<String, Object> extraData) {
    return baseStack;
  }

  static class TransactionComparator implements Comparator<Transaction> {

    @Override
    public int compare(Transaction obj1, Transaction obj2) {
      if (obj1 == obj2) {
        return 0;
      }

      if (obj1 == null) {
        return -1;
      }

      if (obj2 == null) {
        return 1;
      }

      return obj2.getTransactionDate().compareTo(obj1.getTransactionDate());
    }
  }
}
