package com.maldahleh.stockmarket.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.StockMarket;
import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.CommandManager;
import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.commands.subcommands.types.HelpCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.broker.SpawnSimpleBrokerCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.CompareCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.HistoryCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.ListCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.LookupCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.PortfolioCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.TransactionsCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.TutorialCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.transactions.BuyCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.transactions.SellCommand;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import com.maldahleh.stockmarket.processor.StockProcessor;
import com.maldahleh.stockmarket.transactions.Transaction;
import com.maldahleh.stockmarket.transactions.types.TransactionType;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MessagesTests {

  private StockMarket stockMarket;
  private Settings settings;
  private Messages messages;

  @BeforeEach
  void setup() {
    this.stockMarket = mockedStockMarket();
    this.settings = mock(Settings.class);
    this.messages = new Messages(stockMarket, settings);
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
    messages.sendCooldownMessage(player);

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

  @Test
  void noContent() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendNoContent(player);

    // THEN
    verify(player)
        .sendMessage(color("&6No data found!"));
  }

  @Test
  void citizensRequired() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendCitizensRequired(player);

    // THEN
    verify(player)
        .sendMessage(color("&cCitizens is not enabled, and is required for brokers."));
  }

  @Test
  void playerOnly() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendPlayerOnly(player);

    // THEN
    verify(player)
        .sendMessage(color("&cYou must be a player to use this command."));
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

    @Nested
    class Help {

      @BeforeEach
      void setup() {
        CommandManager commandManager = mock(CommandManager.class);

        InventoryManager inventoryManager = mock(InventoryManager.class);
        StockProcessor purchaseProcessor = mock(StockProcessor.class);
        StockProcessor saleProcessor = mock(StockProcessor.class);
        BrokerManager brokerManager = mock(BrokerManager.class);

        List<Subcommand> subcommandList = List.of(
            new HelpCommand(messages),
            new TutorialCommand(inventoryManager),
            new ListCommand(inventoryManager),
            new LookupCommand(inventoryManager, messages),
            new CompareCommand(inventoryManager, messages),
            new PortfolioCommand(stockMarket, inventoryManager, messages),
            new TransactionsCommand(stockMarket, inventoryManager, messages),
            new HistoryCommand(inventoryManager, messages),
            new BuyCommand(purchaseProcessor, messages),
            new SellCommand(saleProcessor, messages),
            new SpawnSimpleBrokerCommand(brokerManager, messages)
        );

        when(commandManager.getRegisteredSubcommands())
            .thenReturn(subcommandList);

        when(stockMarket.getCommandManager())
            .thenReturn(commandManager);
      }

      @Test
      void all() {
        // GIVEN
        Player player = mock(Player.class);

        when(player.hasPermission("stockmarket.tutorial"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.list"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.lookup"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.compare"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.portfolio"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.portfolio.other"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.transactions"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.transactions.other"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.history"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.simplebroker"))
            .thenReturn(true);

        // WHEN
        messages.sendHelpMessage(player);

        // THEN
        verify(player)
            .sendMessage(color(
                "&eWe run a virtual stock market, for a list of stocks you can buy, visit &6Yahoo Finance&e."));
        verify(player)
            .sendMessage(color(
                "&eStocks are a great way to earn money by investing in virtual stocks in companies you think will be successful on the stock market."));
        verify(player)
            .sendMessage(color("&eYou can learn about stocks by using &6/stocks tutorial&e."));
        verify(player)
            .sendMessage(color(" "));
        verify(player)
            .sendMessage(color("&6&lCommands:"));
        verify(player)
            .sendMessage(color("&e/stockmarket help &6- &eDisplay this message"));
        verify(player)
            .sendMessage(color("&e/stockmarket list &6- &eDisplay a list of popular stocks"));
        verify(player)
            .sendMessage(color("&e/stockmarket tutorial &6- &eLearn about the Stock Market"));
        verify(player)
            .sendMessage(color("&e/stockmarket lookup {symbol} &6- &eView information on a stock"));
        verify(player)
            .sendMessage(color(
                "&e/stockmarket compare {comma separated list of symbols}  &6- &eCompare multiple stocks"));
        verify(player)
            .sendMessage(color("&e/stockmarket portfolio &6- &eView your stock portfolio"));
        verify(player)
            .sendMessage(
                color("&e/stockmarket portfolio {player} &6- &eView another player's portfolio"));
        verify(player)
            .sendMessage(color("&e/stockmarket transactions &6- &eView your transaction history"));
        verify(player)
            .sendMessage(color(
                "&e/stockmarket transactions {player} &6- &eView another player's transaction history"));
        verify(player)
            .sendMessage(color(
                "&e/stockmarket history &6- &eView the server's 100 most recent transactions"));
        verify(player)
            .sendMessage(color(
                "&e/stockmarket history {symbol} &6- &eView a symbol's server wide transactions"));
        verify(player)
            .sendMessage(color("&e/stockmarket buy {symbol} {amount} &6- &eBuy a stock"));
        verify(player)
            .sendMessage(color("&e/stockmarket sell {symbol} {amount} &6- &eSell a stock"));
        verify(player)
            .sendMessage(color("&e/stockmarket spawnsimplebroker &6- &eSpawn a simple broker"));
      }

      @Test
      void someMissingPerms() {
        // GIVEN
        Player player = mock(Player.class);

        when(player.hasPermission("stockmarket.tutorial"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.list"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.lookup"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.compare"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.portfolio"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.portfolio.other"))
            .thenReturn(false);

        when(player.hasPermission("stockmarket.transactions"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.transactions.other"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.history"))
            .thenReturn(true);

        when(player.hasPermission("stockmarket.simplebroker"))
            .thenReturn(false);

        // WHEN
        messages.sendHelpMessage(player);

        // THEN
        verify(player)
            .sendMessage(color(
                "&eWe run a virtual stock market, for a list of stocks you can buy, visit &6Yahoo Finance&e."));
        verify(player)
            .sendMessage(color(
                "&eStocks are a great way to earn money by investing in virtual stocks in companies you think will be successful on the stock market."));
        verify(player)
            .sendMessage(color("&eYou can learn about stocks by using &6/stocks tutorial&e."));
        verify(player)
            .sendMessage(color(" "));
        verify(player)
            .sendMessage(color("&6&lCommands:"));
        verify(player)
            .sendMessage(color("&e/stockmarket help &6- &eDisplay this message"));
        verify(player)
            .sendMessage(color("&e/stockmarket list &6- &eDisplay a list of popular stocks"));
        verify(player)
            .sendMessage(color("&e/stockmarket tutorial &6- &eLearn about the Stock Market"));
        verify(player)
            .sendMessage(color("&e/stockmarket lookup {symbol} &6- &eView information on a stock"));
        verify(player)
            .sendMessage(color(
                "&e/stockmarket compare {comma separated list of symbols}  &6- &eCompare multiple stocks"));
        verify(player)
            .sendMessage(color("&e/stockmarket portfolio &6- &eView your stock portfolio"));
        verify(player)
            .sendMessage(color("&e/stockmarket transactions &6- &eView your transaction history"));
        verify(player)
            .sendMessage(color(
                "&e/stockmarket transactions {player} &6- &eView another player's transaction history"));
        verify(player)
            .sendMessage(color(
                "&e/stockmarket history &6- &eView the server's 100 most recent transactions"));
        verify(player)
            .sendMessage(color(
                "&e/stockmarket history {symbol} &6- &eView a symbol's server wide transactions"));
        verify(player)
            .sendMessage(color("&e/stockmarket buy {symbol} {amount} &6- &eBuy a stock"));
        verify(player)
            .sendMessage(color("&e/stockmarket sell {symbol} {amount} &6- &eSell a stock"));
      }
    }

    @Test
    void boughtStock() {
      // GIVEN
      Player player = mock(Player.class);
      String company = "Boeing";
      Transaction transaction = Transaction.builder()
          .uuid(UUID.randomUUID())
          .type(TransactionType.PURCHASE)
          .symbol("BA")
          .quantity(2)
          .singlePrice(BigDecimal.valueOf(5))
          .brokerFee(BigDecimal.TEN)
          .grandTotal(BigDecimal.valueOf(20))
          .build();

      when(settings.format(transaction.getSinglePrice()))
          .thenReturn("5.00");

      when(settings.format(transaction.getBrokerFee()))
          .thenReturn("10.00");

      when(settings.format(transaction.getGrandTotal()))
          .thenReturn("20.00");

      when(settings.format(transaction.getEarnings()))
          .thenReturn("N/A");

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

      when(settings.format(transaction.getSinglePrice()))
          .thenReturn("5.00");

      when(settings.format(transaction.getBrokerFee()))
          .thenReturn("10.00");

      when(settings.format(transaction.getGrandTotal()))
          .thenReturn("20.00");

      when(settings.format(transaction.getEarnings()))
          .thenReturn("100.00");

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
