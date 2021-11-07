package com.maldahleh.stockmarket.commands.subcommand.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.types.transactions.SellCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.types.SaleProcessor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class SellCommandTests {

    private Messages messages;

    private SellCommand command;

    @BeforeEach
    void setup() {
        SaleProcessor saleProcessor = mock(SaleProcessor.class);
        messages = mock(Messages.class);

        command = new SellCommand(saleProcessor, messages);
    }

    @Test
    void hasRightProperties() {
        // GIVEN
        Player player = mock(Player.class);

        // THEN
        assertEquals(2, command.minArgs());
        assertEquals(3, command.maxArgs());
        assertEquals("sell", command.commandName());
        assertNull(command.requiredPerm());

        assertEquals(1, command.commandHelpKeys(player).size());
        assertEquals("sell", command.commandHelpKeys(player).get(0));
    }

    @Test
    void processTransaction() {
        // GIVEN
        Player player = mock(Player.class);

        // WHEN
        command.sendTransactionMessage(player);

        // THEN
        verify(messages, times(1))
                .sendPendingSale(player);
    }
}
