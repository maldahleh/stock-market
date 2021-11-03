package com.maldahleh.stockmarket.stocks;

import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.stocks.provider.ForexProvider;
import com.maldahleh.stockmarket.stocks.provider.MarketStatusProvider;
import com.maldahleh.stockmarket.stocks.provider.StockProvider;
import java.math.BigDecimal;
import org.bukkit.entity.Player;
import yahoofinance.Stock;

public class StockManager {

  private final Settings settings;
  private final ForexProvider forexProvider;
  private final MarketStatusProvider marketStatusProvider;
  private final StockProvider stockProvider;

  public StockManager(Settings settings) {
    this.settings = settings;
    this.forexProvider = new ForexProvider(settings);
    this.marketStatusProvider = new MarketStatusProvider(settings);
    this.stockProvider = new StockProvider(settings);
  }

  public void cacheStocks(String... symbols) {
    stockProvider.get(symbols);
  }

  public Stock getStock(String symbol) {
    return stockProvider.get(symbol);
  }

  public BigDecimal getServerPrice(String symbol) {
    Stock stock = getStock(symbol);
    if (stock == null) {
      return null;
    }

    return getServerPrice(stock);
  }

  public BigDecimal getServerPrice(Stock stock) {
    BigDecimal price = stock.getQuote().getPrice().multiply(settings.getPriceMultiplier());
    if (stock.getCurrency().equalsIgnoreCase(ForexProvider.USD)) {
      return price;
    }

    BigDecimal conversionFactor = forexProvider.getFxRate(stock.getCurrency());
    if (conversionFactor == null) {
      return null;
    }

    return price.multiply(conversionFactor);
  }

  public boolean canNotUseStock(Player player, Stock stock, Settings settings, Messages messages) {
    if (stock == null || stock.getName().equalsIgnoreCase("N/A")) {
      messages.sendInvalidStock(player);
      return true;
    }

    if (!settings.isAllowedCurrency(stock.getCurrency())
        || !settings.isAllowedExchange(stock.getStockExchange())) {
      messages.sendDisabledStock(player);
      return true;
    }

    if (!settings.isAboveMinimumPrice(stock.getQuote().getPrice())) {
      messages.sendLowPriceStock(player);
      return true;
    }

    if (settings.isBlockTransactionsWhenClosed()
        && !marketStatusProvider.get(stock.getSymbol())) {
      messages.sendMarketClosed(player);
      return true;
    }

    return false;
  }
}
