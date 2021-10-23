package com.maldahleh.stockmarket.commands.subcommand.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maldahleh.stockmarket.commands.subcommands.common.TransactionCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class TransactionCommandTests {

  @Test
  void invalidQuantity() {
    // GIVEN
    String[] args = new String[]{"test", "BA", "-5"};

    Player player = mock(Player.class);
    Messages messages = mock(Messages.class);
    StockProcessor processor = mock(StockProcessor.class);

    // WHEN
    TransactionCommand transactionCommand = new TransactionCommand(processor, messages) {
      @Override
      public void processTransaction(Player player, String symbol, int quantity) {

      }

      @Override
      public String commandName() {
        return "transaction";
      }
    };

    transactionCommand.onCommand(player, args);

    // THEN
    assertEquals(2, transactionCommand.minArgs());
    assertEquals(3, transactionCommand.maxArgs());
    assertNull(transactionCommand.requiredPerm());

    verify(messages, times(1))
        .sendInvalidQuantity(player);
  }

  @Test
  void validQuantity() {
    // GIVEN
    String[] args = new String[]{"test", "BA", "5"};

    Player player = mock(Player.class);
    Messages messages = mock(Messages.class);
    StockProcessor processor = mock(StockProcessor.class);

    // WHEN
    TransactionCommand transactionCommand = spy(new TransactionCommand(processor, messages) {
      @Override
      public void processTransaction(Player player, String symbol, int quantity) {

      }

      @Override
      public String commandName() {
        return "transaction";
      }
    });

    transactionCommand.onCommand(player, args);

    // THEN
    assertEquals(2, transactionCommand.minArgs());
    assertEquals(3, transactionCommand.maxArgs());
    assertNull(transactionCommand.requiredPerm());

    verify(transactionCommand, times(1))
        .processTransaction(player, "BA", 5);
  }
}
