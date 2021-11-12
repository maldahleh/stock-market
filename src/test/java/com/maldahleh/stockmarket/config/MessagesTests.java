package com.maldahleh.stockmarket.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.transactions.Transaction;
import java.io.File;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MessagesTests {

  private Settings settings;
  private Messages messages;

  @BeforeEach
  void setup() {
    this.settings = mock(Settings.class);
    this.messages = new Messages(mockedStockMarket(), settings);
  }

  @Test
  void commandsDisabled() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendCommandsDisabled(player);

    // THEN
    verify(player)
        .sendMessage(color("&cCommands are disabled, please use the stock brokers."));
  }

  @Test
  void lowPriceStock() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendLowPriceStock(player);

    // THEN
    verify(player)
        .sendMessage(color("&eThe specified stock has a price which is too low."));
  }

  @Test
  void disabledStock() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendDisabledStock(player);

    // THEN
    verify(player)
        .sendMessage(color("&eYou can only lookup stocks in the US and Canadian stock markets."));
  }

  @Test
  void compareMax() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendCompareMax(player);

    // THEN
    verify(player)
        .sendMessage(color("&eYou can only compare up to 3 stocks at a time."));
  }

  @Test
  void cooldownMessage() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendCompareMax(player);

    // THEN
    verify(player)
        .sendMessage(color("&eYou have to wait 2 seconds between transactions."));
  }

  @Test
  void invalidStock() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendInvalidStock(player);

    // THEN
    verify(player)
        .sendMessage(color("&eYou specified an invalid stock symbol."));
  }

  @Test
  void invalidQuantity() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendInvalidQuantity(player);

    // THEN
    verify(player)
        .sendMessage(color("&eYou specified an invalid quantity."));
  }

  @Test
  void invalidSale() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendInvalidSale(player);

    // THEN
    verify(player)
        .sendMessage(color("&eYou do not have enough of that stock to sell."));
  }

  @Test
  void noFunds() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendInsufficientFunds(player);

    // THEN
    verify(player)
        .sendMessage(color("&eYou do not have enough money to make that purchase."));
  }

  @Test
  void invalidSyntax() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendInvalidSyntax(player);

    // THEN
    verify(player)
        .sendMessage(color("&eInvalid syntax! Use &6/stocks help &efor help."));
  }

  @Test
  void noPermission() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendNoPermission(player);

    // THEN
    verify(player)
        .sendMessage(color("&eYou do not have permission to use that command."));
  }

  @Nested
  class Pending {

    @Test
    void lookup() {
      // GIVEN
      Player player = mock(Player.class);

      // WHEN
      messages.sendPendingLookup(player);

      // THEN
      verify(player)
          .sendMessage(color("&eLooking up stock..."));
    }

    @Test
    void compare() {
      // GIVEN
      Player player = mock(Player.class);

      // WHEN
      messages.sendPendingCompare(player);

      // THEN
      verify(player)
          .sendMessage(color("&eLooking up stocks..."));
    }

    @Test
    void portfolio() {
      // GIVEN
      Player player = mock(Player.class);

      // WHEN
      messages.sendPendingPortfolio(player);

      // THEN
      verify(player)
          .sendMessage(color("&eGenerating portfolio..."));
    }

    @Test
    void portfolioOther() {
      // GIVEN
      Player player = mock(Player.class);

      // WHEN
      messages.sendPendingPortfolioOther(player);

      // THEN
      verify(player)
          .sendMessage(color("&eGenerating portfolio..."));
    }

    @Test
    void transactions() {
      // GIVEN
      Player player = mock(Player.class);

      // WHEN
      messages.sendPendingTransactions(player);

      // THEN
      verify(player)
          .sendMessage(color("&eGenerating transaction history..."));
    }

    @Test
    void transactionsOther() {
      // GIVEN
      Player player = mock(Player.class);

      // WHEN
      messages.sendPendingTransactionsOther(player);

      // THEN
      verify(player)
          .sendMessage(color("&eGenerating transaction history..."));
    }

    @Test
    void history() {
      // GIVEN
      Player player = mock(Player.class);

      // WHEN
      messages.sendPendingHistory(player);

      // THEN
      verify(player)
          .sendMessage(color("&eGenerating history..."));
    }

    @Test
    void historySymbol() {
      // GIVEN
      Player player = mock(Player.class);

      // WHEN
      messages.sendPendingHistorySymbol(player);

      // THEN
      verify(player)
          .sendMessage(color("&eGenerating symbol history..."));
    }

    @Test
    void buy() {
      // GIVEN
      Player player = mock(Player.class);

      // WHEN
      messages.sendPendingBuy(player);

      // THEN
      verify(player)
          .sendMessage(color("&eProcessing purchase..."));
    }

    @Test
    void sale() {
      // GIVEN
      Player player = mock(Player.class);

      // WHEN
      messages.sendPendingSale(player);

      // THEN
      verify(player)
          .sendMessage(color("&eProcessing sale..."));
    }
  }

  @Nested
  class MultiLine {

    @BeforeEach
    void setup() {
      when(settings.getLocale())
          .thenReturn(Locale.US);
    }

    @Test
    void boughtStock() {
      // GIVEN
      Player player = mock(Player.class);
      String company = "Boeing";
      Transaction transaction = Transaction.buildPurchase(
          UUID.randomUUID(),
          "BA",
          2,
          BigDecimal.valueOf(5),
          BigDecimal.TEN,
          BigDecimal.valueOf(20)
      );

      // WHEN
      messages.sendBoughtStockMessage(player, company, transaction);

      // THEN
      verify(player)
          .sendMessage(color("&6You purchased &e2 &6of &eBoeing &6(&eBA&6)"));
      verify(player)
          .sendMessage(color("&6Stock Value: &e5.00 &6(&ex2&6)"));
      verify(player)
          .sendMessage(color("&6Broker Fees: &e10.00"));
      verify(player)
          .sendMessage(color("&6Grand Total: &e20.00"));
    }

    @Test
    void soldStock() {
      // GIVEN
      Player player = mock(Player.class);
      String company = "Boeing";
      Transaction transaction = Transaction.buildSale(
          UUID.randomUUID(),
          "BA",
          2,
          BigDecimal.valueOf(5),
          BigDecimal.TEN,
          BigDecimal.valueOf(100),
          BigDecimal.valueOf(20)
      );

      // WHEN
      messages.sendSoldStockMessage(player, company, transaction);

      // THEN
      verify(player)
          .sendMessage(color("&6You sold &e2 &6of &eBoeing &6(&eBA&6)"));
      verify(player)
          .sendMessage(color("&6Stock Value: &e5.00 &6(&ex2&6)"));
      verify(player)
          .sendMessage(color("&6Broker Fees: &e10.00"));
      verify(player)
          .sendMessage(color("&6Grand Total: &e20.00"));
      verify(player)
          .sendMessage(color("&6Net: &e100.00"));
    }
  }

  private String color(String message) {
    return ChatColor.translateAlternateColorCodes('&', message);
  }

  private StockMarket mockedStockMarket() {
    StockMarket stockMarket = mock(StockMarket.class);

    when(stockMarket.getDataFolder())
        .thenReturn(new File("src/test/resources"));

    return stockMarket;
  }
}
