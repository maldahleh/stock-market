package com.maldahleh.stockmarket.inventories.portfolio.provider;

import com.google.common.collect.ImmutableMap;
import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.utils.paged.provider.IContentProvider;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.players.player.data.StockData;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.Utils;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.bukkit.inventory.ItemStack;
import yahoofinance.Stock;

@AllArgsConstructor
public class PortfolioInventoryProvider implements IContentProvider<String, StockData, Stock> {
  private final StockMarket stockMarket;
  private final PlayerManager playerManager;
  private final StockManager stockManager;
  private final Settings settings;

  @Override
  public Map<String, StockData> getContent(UUID uuid) {
    StockPlayer stockPlayer = playerManager.forceGetStockPlayer(uuid);
    if (stockPlayer == null) {
      return null;
    }

    return stockPlayer.getStockMap();
  }

  @Override
  public Map<Stock, StockData> applyTransformations(Map<String, StockData> data) {
    stockManager.cacheStocks(data.keySet().toArray(new String[0]));

    Map<Stock, StockData> stockDataMap = new TreeMap<>(new StockComparator());
    for (Map.Entry<String, StockData> e : data.entrySet()) {
      if (e.getValue().getQuantity() == 0) {
        continue;
      }

      stockDataMap.put(stockManager.getStock(e.getKey()), e.getValue());
    }

    return stockDataMap;
  }

  @Override
  public Map<String, Object> getExtraData(UUID uuid) {
    Map<String, Object> dataMap = new HashMap<>();
    StockPlayer stockPlayer = playerManager.forceGetStockPlayer(uuid);
    if (stockPlayer == null) {
      return dataMap;
    }

    BigDecimal currentValue = BigDecimal.ZERO;
    for (Map.Entry<String, StockData> e : stockPlayer.getStockMap().entrySet()) {
      Stock stock = stockManager.getStock(e.getKey());
      if (stock == null) {
        continue;
      }

      BigDecimal serverPrice = stockManager.getServerPrice(stock, settings.getPriceMultiplier());
      if (serverPrice == null) {
        continue;
      }

      currentValue = currentValue.add(serverPrice.multiply(BigDecimal.valueOf(e.getValue()
          .getQuantity())));
    }

    BigDecimal net = currentValue.subtract(stockPlayer.getPortfolioValue());
    dataMap.put("purchase_value", stockPlayer.getPortfolioValue());
    dataMap.put("current_value", currentValue);
    dataMap.put("net_value", net);
    return dataMap;
  }

  @Override
  public ItemStack getContentStack(ItemStack baseStack, int position, Stock key, StockData value) {
    BigDecimal currentPrice = stockManager.getServerPrice(key, settings.getPriceMultiplier());
    if (currentPrice == null) {
      return baseStack;
    }

    currentPrice = currentPrice.multiply(BigDecimal.valueOf(value.getQuantity()));
    BigDecimal net = value.getValue().subtract(currentPrice);
    return Utils.updateItemStack(baseStack.clone(), ImmutableMap.<String, Object>builder()
        .put("<symbol>", key.getSymbol().toUpperCase())
        .put("<name>", key.getName())
        .put("<quantity>", value.getQuantity())
        .put("<current-value>", Utils.formatCurrency(currentPrice.doubleValue(),
            settings.getLocale()))
        .put("<purchase-value>", Utils.formatCurrency(value.getValue().doubleValue(), settings
            .getLocale()))
        .put("<net>", Utils.formatCurrency(net.doubleValue(), settings.getLocale()))
        .put("<server-currency>", stockMarket.getEcon().currencyNamePlural())
        .build());
  }

  @Override
  public ItemStack getExtraItem(ItemStack baseStack, Map<String, Object> extraData) {
    return Utils.updateItemStack(baseStack.clone(), ImmutableMap.of(
        "<purchase-value>", Utils.formatCurrency(((BigDecimal) extraData.get("purchase_value"))
            .doubleValue(), settings.getLocale()),
        "<current-value>", Utils.formatCurrency(((BigDecimal) extraData.get("current_value"))
            .doubleValue(), settings.getLocale()),
        "<net-value>", Utils.formatCurrency(((BigDecimal) extraData.get("net_value"))
            .doubleValue(), settings.getLocale()),
        "<server-currency>", stockMarket.getEcon().currencyNamePlural()
    ));
  }

  class StockComparator implements Comparator<Stock> {
    @Override
    public int compare(Stock obj1, Stock obj2) {
      if (obj1 == obj2) {
        return 0;
      }

      if (obj1 == null) {
        return -1;
      }

      if (obj2 == null) {
        return 1;
      }

      return obj1.getSymbol().toUpperCase().compareTo(obj2.getSymbol().toUpperCase());
    }
  }
}
