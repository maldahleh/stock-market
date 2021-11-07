package com.maldahleh.stockmarket.commands.subcommand.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.types.transactions.SellCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import com.maldahleh.stockmarket.processor.types.SaleProcessor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class SellCommandTests {

    private SaleProcessor saleProcessor;
    private Messages messages;

    private SellCommand command;

    @BeforeEach
    void setup() {
        PurchaseProcessor purchaseProcessor = mock(PurchaseProcessor.class);

        saleProcessor = mock(SaleProcessor.class);
        messages = mock(Messages.class);

        command = new SellCommand(purchaseProcessor, saleProcessor, messages);
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
        String symbol = "BA";
        int quantity = 1;

        // WHEN
        command.processTransaction(player, symbol, quantity);

        // THEN
        verify(messages, times(1))
                .sendPendingSale(player);

        verify(saleProcessor, times(1))
                .processTransaction(player, symbol, quantity);
    }
}
