package com.maldahleh.stockmarket.commands.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.CommandManager;
import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.config.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StockMarketCommandTests {

  private static final String SUBCOMMAND_NAME = "test";
  private static final String SUBCOMMAND_PERM = "test.name";

  private static final String[] ARGS = new String[]{SUBCOMMAND_NAME};

  private final CommandManager commandManager = mock(CommandManager.class);
  private final BrokerManager brokerManager = mock(BrokerManager.class);
  private final Messages messages = mock(Messages.class);

  private final Command command = mock(Command.class);

  private final StockMarketCommand stockMarketCommand = new StockMarketCommand(commandManager,
      brokerManager, messages);

  @Test
  void commandSenderNotPlayer() {
    // GIVEN
    ConsoleCommandSender commandSender = mock(ConsoleCommandSender.class);

    // WHEN
    stockMarketCommand.onCommand(commandSender, command, "", ARGS);

    // THEN
    verify(messages)
        .sendPlayerOnly(commandSender);
  }

  @Test
  void commandsDisabled() {
    // GIVEN
    Player player = mock(Player.class);

    when(player.hasPermission("stockmarket.use"))
        .thenReturn(true);

    when(brokerManager.areCommandsDisabled(player))
        .thenReturn(true);

    // WHEN
    stockMarketCommand.onCommand(player, command, "", ARGS);

    // THEN
    verify(messages)
        .sendCommandsDisabled(player);
  }

  @Test
  void noArgsSendHelpMessage() {
    // GIVEN
    Player player = mock(Player.class);

    when(player.hasPermission("stockmarket.use"))
        .thenReturn(true);

    // WHEN
    stockMarketCommand.onCommand(player, command, "", new String[0]);

    // THEN
    verify(messages)
        .sendHelpMessage(player);
  }

  @Nested
  @DisplayName("Subcommands")
  class SubcommandLogic {

    private Subcommand subcommand;

    @BeforeEach
    void beforeEach() {
      subcommand = spy(new BaseCommand(messages) {
        @Override
        public void onCommand(Player player, String[] args) {
          // implementation not tested
        }

        @Override
        public int maxArgs() {
          return 2;
        }

        @Override
        public String requiredPerm() {
          return SUBCOMMAND_PERM;
        }

        @Override
        public String commandName() {
          return null;
        }
      });
    }

    @Test
    void notFound() {
      // GIVEN
      Player player = mock(Player.class);

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      // WHEN
      stockMarketCommand.onCommand(player, command, "", ARGS);

      // THEN
      verify(messages)
          .sendInvalidSyntax(player);
    }

    @Test
    void noPermission() {
      // GIVEN
      Player player = mock(Player.class);

      when(commandManager.findSubcommand(SUBCOMMAND_NAME))
          .thenReturn(subcommand);

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      when(player.hasPermission(SUBCOMMAND_PERM))
          .thenReturn(false);

      // WHEN
      stockMarketCommand.onCommand(player, command, "", ARGS);

      // THEN
      verify(messages)
          .sendNoPermission(player);
    }

    @Test
    void noGlobalPermission() {
      // GIVEN
      Player player = mock(Player.class);

      when(commandManager.findSubcommand(SUBCOMMAND_NAME))
          .thenReturn(subcommand);

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(false);

      // WHEN
      stockMarketCommand.onCommand(player, command, "", ARGS);

      // THEN
      verify(messages)
          .sendNoPermission(player);
    }

    @Test
    void invalidSyntax() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[]{SUBCOMMAND_NAME, SUBCOMMAND_NAME, SUBCOMMAND_NAME};

      when(commandManager.findSubcommand(SUBCOMMAND_NAME))
          .thenReturn(subcommand);

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      when(player.hasPermission(SUBCOMMAND_PERM))
          .thenReturn(true);

      // WHEN
      stockMarketCommand.onCommand(player, command, "", args);

      // THEN
      verify(messages)
          .sendInvalidSyntax(player);
    }

    @Test
    void correctSyntax() {
      // GIVEN
      Player player = mock(Player.class);

      when(commandManager.findSubcommand(SUBCOMMAND_NAME))
          .thenReturn(subcommand);

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      when(player.hasPermission(SUBCOMMAND_PERM))
          .thenReturn(true);

      // WHEN
      stockMarketCommand.onCommand(player, command, "", ARGS);

      // THEN
      verify(subcommand)
          .onCommand(player, ARGS);
    }

    @Test
    void correctSyntaxNoPermSubcommand() {
      // GIVEN
      Player player = mock(Player.class);

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      when(subcommand.requiredPerm())
          .thenReturn(null);

      when(commandManager.findSubcommand(SUBCOMMAND_NAME))
          .thenReturn(subcommand);

      // WHEN
      stockMarketCommand.onCommand(player, command, "", ARGS);

      // THEN
      verify(subcommand)
          .onCommand(player, ARGS);
    }
  }
}
