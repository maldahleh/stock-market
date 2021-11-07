package com.maldahleh.stockmarket.commands.subcommand.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.types.transactions.BuyCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import com.maldahleh.stockmarket.processor.types.SaleProcessor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class BuyCommandTests {

    private PurchaseProcessor purchaseProcessor;
    private Messages messages;

    private BuyCommand command;

    @BeforeEach
    void setup() {
        SaleProcessor saleProcessor = mock(SaleProcessor.class);

        purchaseProcessor = mock(PurchaseProcessor.class);
        messages = mock(Messages.class);

        command = new BuyCommand(purchaseProcessor, saleProcessor, messages);
    }

    @Test
    void hasRightProperties() {
        // GIVEN
        Player player = mock(Player.class);

        // THEN
        assertEquals(2, command.minArgs());
        assertEquals(3, command.maxArgs());
        assertEquals("buy", command.commandName());
        assertNull(command.requiredPerm());

        assertEquals(1, command.commandHelpKeys(player).size());
        assertEquals("buy", command.commandHelpKeys(player).get(0));
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
                .sendPendingBuy(player);

        verify(purchaseProcessor, times(1))
                .processTransaction(player, symbol, quantity);
    }
}
