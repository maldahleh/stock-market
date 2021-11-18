package com.maldahleh.stockmarket.inventories.history.providers;

import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.utils.paged.provider.ContentProvider;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.utils.Utils;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

public class StockHistoryProvider extends ContentProvider<String, Transaction, OfflinePlayer> {

  private final Storage storage;

  public StockHistoryProvider(StockMarket stockMarket, Storage storage, Settings settings) {
    super(stockMarket, settings);

    this.storage = storage;
  }

  @Override
  public Map<Transaction, OfflinePlayer> getContent(String lookup) {
    Map<Transaction, UUID> data = getData(lookup);
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
  public ItemStack getContentStack(ItemStack baseStack, Transaction key, OfflinePlayer value) {
    return Utils.updateItemStack(
        baseStack,
        ImmutableMap.<String, Object>builder()
            .put("<name>", value.getName())
            .put("<date>", formatInstant(key.getTransactionDate()))
            .put("<symbol>", key.getSymbol().toUpperCase())
            .put("<transaction-type>", key.getTransactionType())
            .put("<quantity>", key.getQuantity())
            .put("<stock-value>", settings.format(key.getStockValue()))
            .put("<broker-fees>", settings.format(key.getBrokerFee()))
            .put("<grand-total>", settings.format(key.getGrandTotal()))
            .put("<earnings>", settings.format(key.getEarnings()))
            .put("<server-currency>", stockMarket.getEcon().currencyNamePlural())
            .put("<sold>", String.valueOf(key.isSold()))
            .build()
    );
  }

  private Map<Transaction, UUID> getData(String key) {
    if (key == null) {
      return storage.getTransactionHistory().stream()
          .collect(Collectors.toMap(t -> t, Transaction::getUuid));
    }

    return storage.getStockTransactions(key.toUpperCase()).stream()
        .collect(Collectors.toMap(t -> t, Transaction::getUuid));
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
