package com.maldahleh.stockmarket.commands.subcommand.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.types.transactions.BuyCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.StockProcessor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class BuyCommandTests {

    private StockProcessor stockProcessor;
    private Messages messages;

    private BuyCommand command;

    @BeforeEach
    void setup() {
        stockProcessor = mock(StockProcessor.class);
        messages = mock(Messages.class);

        command = new BuyCommand(stockProcessor, messages);
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

        verify(stockProcessor, times(1))
                .buyStock(player, symbol, quantity);
    }
}
