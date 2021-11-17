package com.maldahleh.stockmarket.commands.impl;

import com.maldahleh.stockmarket.commands.CommandManager;
import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.commands.subcommands.common.TargetableCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
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
    this.command = mock(Command.class);
    this.commandManager = mock(CommandManager.class);

    this.stockMarketTabCompleter = new StockMarketTabCompleter(commandManager);
  }

  @Test
  void consoleSender() {
    // GIVEN
    ConsoleCommandSender commandSender = mock(ConsoleCommandSender.class);
    String[] args = new String[]{};

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
    String[] args = new String[]{};

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
    String[] args = new String[]{"test1", "test2", "test3"};

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
    String[] args = new String[]{"test1"};

    when(player.hasPermission("stockmarket.use"))
        .thenReturn(false);

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

    @BeforeEach
    void setupManager() {
      Collection<Subcommand> subcommands = buildSubcommandCollection();
      when(commandManager.getRegisteredSubcommands())
          .thenReturn(subcommands);
    }

    @Test
    void portfolio() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[]{"portfolio", "a"};

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      when(player.hasPermission("stockmarket.portfolio.other"))
          .thenReturn(true);

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
      String[] args = new String[]{"transactions", "a"};

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      when(player.hasPermission("stockmarket.transactions.other"))
          .thenReturn(true);

      // WHEN
      List<String> completed =
          stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

      // THEN
      assertNull(completed);
    }

    @Test
    void noPlayerListSupport() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[]{"dummy", "a"};

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      when(player.hasPermission("stockmarket.dummy.other"))
          .thenReturn(true);

      // WHEN
      List<String> completed =
          stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

      // THEN
      assertNotNull(completed);
      assertTrue(completed.isEmpty());
    }

    @Test
    void noPermission() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[]{"portfolio", "a"};

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      when(player.hasPermission("stockmarket.portfolio.other"))
          .thenReturn(false);

      // WHEN
      List<String> completed =
          stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

      // THEN
      assertNotNull(completed);
      assertTrue(completed.isEmpty());
    }

    private Collection<Subcommand> buildSubcommandCollection() {
      Collection<Subcommand> subcommandCollection = new ArrayList<>();
      subcommandCollection.add(buildTargeted("portfolio", "stockmarket.portfolio"));
      subcommandCollection.add(buildTargeted("transactions", "stockmarket.transactions"));
      subcommandCollection.add(buildBase());

      return subcommandCollection;
    }

    private Subcommand buildBase() {
      return new BaseCommand() {
        @Override
        public void onCommand(Player player, String[] args) {
          // implementation not tested
        }

        @Override
        public String commandName() {
          return "dummy";
        }

        @Override
        public String requiredPerm() {
          return "stockmarket.dummy";
        }
      };
    }

    private Subcommand buildTargeted(String commandName, String permission) {
      return new TargetableCommand(mock(Plugin.class), mock(InventoryManager.class),
          mock(Messages.class)) {
        @Override
        public void callerAction(Player caller) {
          // implementation not tested
        }

        @Override
        public void targetAction(Player caller, UUID target) {
          // implementation not tested
        }

        @Override
        public void onCommand(Player player, String[] args) {
          // implementation not tested
        }

        @Override
        public String commandName() {
          return commandName;
        }

        @Override
        public String requiredPerm() {
          return permission;
        }
      };
    }
  }

  @Nested
  @DisplayName("findPossibleMatches")
  class FindPossibleMatches {

    @BeforeEach
    void setupManager() {
      Collection<Subcommand> subcommands = buildSubcommandCollection();
      when(commandManager.getRegisteredSubcommands())
          .thenReturn(subcommands);
    }

    @Test
    void noPermCommands() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[]{"te"};

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      when(player.hasPermission("test2.perm"))
          .thenReturn(false);

      when(player.hasPermission("test3.perm"))
          .thenReturn(false);

      when(player.hasPermission("activate1.perm"))
          .thenReturn(false);

      // WHEN
      List<String> completed =
          stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

      // THEN
      assertNotNull(completed);
      assertEquals(1, completed.size());
      assertEquals("test1", completed.get(0));
    }

    @Test
    void onlyMatching() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[]{"te"};

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      when(player.hasPermission("test2.perm"))
          .thenReturn(true);

      when(player.hasPermission("test3.perm"))
          .thenReturn(true);

      when(player.hasPermission("activate1.perm"))
          .thenReturn(true);

      // WHEN
      List<String> completed =
          stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

      // THEN
      assertNotNull(completed);
      assertEquals(3, completed.size());
      assertEquals("test1", completed.get(0));
      assertEquals("test2", completed.get(1));
      assertEquals("test3", completed.get(2));
    }

    @Test
    void onlyMatchingOrdered() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[]{"a"};

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      when(player.hasPermission("test2.perm"))
          .thenReturn(true);

      when(player.hasPermission("test3.perm"))
          .thenReturn(true);

      when(player.hasPermission("activate1.perm"))
          .thenReturn(true);

      when(player.hasPermission("activate1.perm.other"))
          .thenReturn(true);

      // WHEN
      List<String> completed =
          stockMarketTabCompleter.onTabComplete(player, command, EMPTY_STRING, args);

      // THEN
      assertNotNull(completed);
      assertEquals(2, completed.size());
      assertEquals("aactivate1", completed.get(0));
      assertEquals("activate1", completed.get(1));
    }

    private Collection<Subcommand> buildSubcommandCollection() {
      Collection<Subcommand> subcommandCollection = new ArrayList<>();
      subcommandCollection.add(buildSubcommand("test1", null));
      subcommandCollection.add(buildSubcommand("test2", "test2.perm"));
      subcommandCollection.add(buildSubcommand("test3", "test3.perm"));
      subcommandCollection.add(buildSubcommand("activate1", "activate1.perm"));
      subcommandCollection.add(buildSubcommand("aactivate1", "activate1.perm.other"));

      return subcommandCollection;
    }

    private Subcommand buildSubcommand(String commandName, String permission) {
      return new BaseCommand() {
        @Override
        public void onCommand(Player player, String[] args) {
          // implementation not tested
        }

        @Override
        public String commandName() {
          return commandName;
        }

        @Override
        public String requiredPerm() {
          return permission;
        }
      };
    }
  }
}
