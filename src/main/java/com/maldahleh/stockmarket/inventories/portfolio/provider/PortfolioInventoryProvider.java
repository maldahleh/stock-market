package com.maldahleh.stockmarket.inventories.portfolio.provider;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.utils.paged.provider.ContentProvider;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.players.player.StockPlayer;
import com.maldahleh.stockmarket.players.player.data.StockData;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.utils.Utils;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import yahoofinance.Stock;

public class PortfolioInventoryProvider extends ContentProvider<UUID, Stock, StockData> {

  private final PlayerManager playerManager;
  private final StockManager stockManager;

  public PortfolioInventoryProvider(StockMarket stockMarket, PlayerManager playerManager,
      StockManager stockManager, Settings settings) {
    super(stockMarket, settings);

    this.playerManager = playerManager;
    this.stockManager = stockManager;
  }

  @Override
  public Map<Stock, StockData> getContent(UUID uuid) {
    Map<String, StockData> data = getData(uuid);
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

    BigDecimal currentValue = playerManager.getCurrentValue(stockPlayer);
    dataMap.put("purchase_value", stockPlayer.getPortfolioValue());
    dataMap.put("current_value", currentValue);
    dataMap.put("net_value", stockPlayer.getProfitMargin(currentValue));
    return dataMap;
  }

  @Override
  public ItemStack getContentStack(ItemStack baseStack, Stock key, StockData value) {
    BigDecimal currentPrice = stockManager.getServerPrice(key);
    if (currentPrice == null) {
      return baseStack;
    }

    currentPrice = currentPrice.multiply(BigDecimal.valueOf(value.getQuantity()));
    BigDecimal net = currentPrice.subtract(value.getValue());
    return Utils.updateItemStack(
        baseStack,
        Map.of(
            "<symbol>", key.getSymbol().toUpperCase(),
            "<name>", key.getName(),
            "<quantity>", value.getQuantity(),
            "<current-value>", settings.format(currentPrice),
            "<purchase-value>", settings.format(value.getValue()),
            "<net>", settings.format(net),
            "<server-currency>", stockMarket.getEcon().currencyNamePlural()
        )
    );
  }

  @Override
  public ItemStack getExtraItem(ItemStack baseStack, Map<String, Object> extraData) {
    return Utils.updateItemStack(
        baseStack,
        Map.of(
            "<purchase-value>", settings.format(((BigDecimal) extraData.get("purchase_value"))),
            "<current-value>", settings.format(((BigDecimal) extraData.get("current_value"))),
            "<net-value>", settings.format(((BigDecimal) extraData.get("net_value"))),
            "<server-currency>", stockMarket.getEcon().currencyNamePlural()
        )
    );
  }

  private Map<String, StockData> getData(UUID uuid) {
    StockPlayer stockPlayer = playerManager.forceGetStockPlayer(uuid);
    if (stockPlayer == null) {
      return Collections.emptyMap();
    }

    return stockPlayer.getStockMap();
  }

  static class StockComparator implements Comparator<Stock> {

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
