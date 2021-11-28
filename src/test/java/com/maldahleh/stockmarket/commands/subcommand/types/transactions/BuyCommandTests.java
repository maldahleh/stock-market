package com.maldahleh.stockmarket.commands.subcommand.types.transactions;

import com.maldahleh.stockmarket.commands.subcommands.types.transactions.BuyCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class BuyCommandTests {

    private BuyCommand command;

    @BeforeEach
    void setup() {
        PurchaseProcessor purchaseProcessor = mock(PurchaseProcessor.class);
        Messages messages = mock(Messages.class);

        command = new BuyCommand(purchaseProcessor, messages);
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
}
