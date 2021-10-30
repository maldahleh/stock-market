package com.maldahleh.stockmarket.commands.impl;

import com.maldahleh.stockmarket.commands.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockMarketTabCompleterTests {

  private static final String EMPTY_STRING = "";

  private Command command;
  private CommandManager commandManager;
  private StockMarketTabCompleter stockMarketTabCompleter;

  @BeforeEach
  void setup() {
    command = mock(Command.class);
    commandManager = mock(CommandManager.class);
    stockMarketTabCompleter = new StockMarketTabCompleter(commandManager);
  }

  @Test
  void consoleSender() {
    // GIVEN
    ConsoleCommandSender commandSender = mock(ConsoleCommandSender.class);
    String[] args = new String[] {};

    // WHEN
    List<String> completed =
        stockMarketTabCompleter.onTabComplete(commandSender, command, EMPTY_STRING, args);

    // THEN
    assertNotNull(completed);
    assertTrue(completed.isEmpty());
  }

  @Test
  void noArgs() {
    // GIVEN
    Player player = mock(Player.class);
    String[] args = new String[] {};

    // WHEN
    List<String> completed =
        stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

    // THEN
    assertNotNull(completed);
    assertTrue(completed.isEmpty());
  }

  @Test
  void tooManyArgs() {
    // GIVEN
    Player player = mock(Player.class);
    String[] args = new String[] {"test1", "test2", "test3"};

    // WHEN
    List<String> completed =
        stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

    // THEN
    assertNotNull(completed);
    assertTrue(completed.isEmpty());
  }

  @Test
  void noBasePerm() {
    // GIVEN
    Player player = mock(Player.class);
    String[] args = new String[] {"test1"};

    when(player.hasPermission("stockmarket.use")).thenReturn(false);

    // WHEN
    List<String> completed =
        stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

    // THEN
    assertNotNull(completed);
    assertTrue(completed.isEmpty());
  }

  @Nested
  @DisplayName("returnPlayerList")
  class ReturnPlayerList {
    @Test
    void portfolio() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[] {"portfolio", "a"};

      when(player.hasPermission("stockmarket.use")).thenReturn(true);

      when(player.hasPermission("stockmarket.portfolio.other")).thenReturn(true);

      // WHEN
      List<String> completed =
          stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

      // THEN
      assertNull(completed);
    }

    @Test
    void transactions() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[] {"transactions", "a"};

      when(player.hasPermission("stockmarket.use")).thenReturn(true);

      when(player.hasPermission("stockmarket.transactions.other")).thenReturn(true);

      // WHEN
      List<String> completed =
          stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

      // THEN
      assertNull(completed);
    }

    @Test
    void noPermission() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[] {"portfolio", "a"};

      when(player.hasPermission("stockmarket.use")).thenReturn(true);

      when(player.hasPermission("stockmarket.portfolio.other")).thenReturn(false);

      // WHEN
      List<String> completed =
          stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

      // THEN
      assertNotNull(completed);
      assertTrue(completed.isEmpty());
    }
  }
}
