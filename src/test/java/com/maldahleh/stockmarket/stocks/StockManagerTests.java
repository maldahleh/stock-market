package com.maldahleh.stockmarket.stocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import java.io.IOException;
import java.math.BigDecimal;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.fx.FxQuote;
import yahoofinance.quotes.stock.StockQuote;

class StockManagerTests {

  private static final String SYMBOL = "BA";

  private Settings settings;
  private Messages messages;

  private StockManager stockManager;

  @BeforeEach
  void setup() {
    this.settings = mock(Settings.class);
    this.messages = mock(Messages.class);

    this.stockManager = new StockManager(settings, messages);
  }

  @Test
  void getServerPrice() {
    try (MockedStatic<YahooFinance> yahooFinance = mockStatic(YahooFinance.class)) {
      // GIVEN
      Stock stock = buildStock();
      stock.setCurrency("USD");

      yahooFinance.when(() -> YahooFinance.get(SYMBOL, true))
          .thenReturn(stock);

      when(settings.getPriceMultiplier())
          .thenReturn(BigDecimal.TEN);

      // WHEN
      stockManager.cacheStocks(SYMBOL);

      BigDecimal serverPrice = stockManager.getServerPrice(SYMBOL);

      // THEN
      assertEquals(BigDecimal.TEN, serverPrice);
    }
  }

  @Nested
  @DisplayName("serverPrice")
  class ServerPrice {

    @Test
    void usdStock() {
      // GIVEN
      Stock stock = buildStock();
      stock.setCurrency("USD");

      when(settings.getPriceMultiplier())
          .thenReturn(BigDecimal.TEN);

      // WHEN
      BigDecimal serverPrice = stockManager.getServerPrice(stock);

      // THEN
      assertEquals(BigDecimal.TEN, serverPrice);
    }

    @Nested
    @DisplayName("nonUsd")
    class NonUSD {

      @Test
      void success() {
        // GIVEN
        Stock stock = buildStock();

        when(settings.getPriceMultiplier())
            .thenReturn(BigDecimal.TEN);

        try (MockedStatic<YahooFinance> yahooFinance = mockStatic(YahooFinance.class)) {
          yahooFinance.when(() -> YahooFinance.getFx("EURUSD=X"))
              .thenReturn(new FxQuote("EURUSD=X", BigDecimal.valueOf(0.5)));

          // WHEN
          BigDecimal serverPrice = stockManager.getServerPrice(stock);

          // THEN
          assertEquals(BigDecimal.valueOf(5.0), serverPrice);
        }
      }

      @Test
      void exception() {
        // GIVEN
        Stock stock = buildStock();

        when(settings.getPriceMultiplier())
            .thenReturn(BigDecimal.TEN);

        try (MockedStatic<YahooFinance> yahooFinance = mockStatic(YahooFinance.class)) {
          yahooFinance.when(() -> YahooFinance.getFx("EURUSD=X"))
              .thenThrow(new IOException());

          // WHEN
          BigDecimal serverPrice = stockManager.getServerPrice(stock);

          // THEN
          assertNull(serverPrice);
        }
      }
    }
  }

  @Nested
  @DisplayName("canNotUseStock")
  class CanNotUseStock {

    Player player = mock(Player.class);

    @Test
    void nullStock() {
      // WHEN
      boolean canNotUse = stockManager.canNotUseStock(player, null);

      // THEN
      assertTrue(canNotUse);

      verify(messages)
          .sendInvalidStock(player);
    }

    @Test
    void naStock() {
      // GIVEN
      Stock stock = buildStock("N/A");

      // WHEN
      boolean canNotUse = stockManager.canNotUseStock(player, stock);

      // THEN
      assertTrue(canNotUse);

      verify(messages)
          .sendInvalidStock(player);
    }

    @Test
    void notAllowedCurrency() {
      // GIVEN
      Stock stock = buildStock();

      when(settings.isAllowedCurrency("EUR"))
          .thenReturn(false);

      // WHEN
      boolean canNotUse = stockManager.canNotUseStock(player, stock);

      // THEN
      assertTrue(canNotUse);

      verify(messages)
          .sendDisabledStock(player);
    }

    @Test
    void notAllowedExchange() {
      // GIVEN
      Stock stock = buildStock();

      when(settings.isAllowedCurrency("EUR"))
          .thenReturn(true);

      when(settings.isAllowedExchange("NYSE"))
          .thenReturn(false);

      // WHEN
      boolean canNotUse = stockManager.canNotUseStock(player, stock);

      // THEN
      assertTrue(canNotUse);

      verify(messages)
          .sendDisabledStock(player);
    }

    @Test
    void priceTooLow() {
      // GIVEN
      Stock stock = buildStock();

      when(settings.isAllowedCurrency("EUR"))
          .thenReturn(true);

      when(settings.isAllowedExchange("NYSE"))
          .thenReturn(true);

      when(settings.isAboveMinimumPrice(BigDecimal.ONE))
          .thenReturn(false);

      // WHEN
      boolean canNotUse = stockManager.canNotUseStock(player, stock);

      // THEN
      assertTrue(canNotUse);

      verify(messages)
          .sendLowPriceStock(player);
    }

    @Test
    void canUse() {
      // GIVEN
      Stock stock = buildStock();

      when(settings.isAllowedCurrency("EUR"))
          .thenReturn(true);

      when(settings.isAllowedExchange("NYSE"))
          .thenReturn(true);

      when(settings.isAboveMinimumPrice(BigDecimal.ONE))
          .thenReturn(true);

      // WHEN
      boolean canNotUse = stockManager.canNotUseStock(player, stock);

      // THEN
      assertFalse(canNotUse);
    }
  }

  private static Stock buildStock() {
    return buildStock(SYMBOL);
  }

  private static Stock buildStock(String name) {
    Stock stock = new Stock(name);
    stock.setName(name);
    stock.setStockExchange("NYSE");
    stock.setCurrency("EUR");

    StockQuote quote = new StockQuote(name);
    quote.setPrice(BigDecimal.ONE);
    stock.setQuote(quote);

    return stock;
  }
}
