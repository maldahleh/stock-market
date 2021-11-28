package com.maldahleh.stockmarket.commands.subcommand.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.types.menus.LookupCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LookupCommandTests {

    private InventoryManager inventoryManager;
    private Messages messages;

    private LookupCommand command;

    @BeforeEach
    void setup() {
        inventoryManager = mock(InventoryManager.class);
        messages = mock(Messages.class);

        command = new LookupCommand(inventoryManager, messages);
    }

    @Test
    void hasRightProperties() {
        // GIVEN
        Player player = mock(Player.class);

        // THEN
        assertEquals(2, command.minArgs());
        assertEquals(2, command.maxArgs());
        assertEquals("stockmarket.lookup", command.requiredPerm());
        assertEquals("lookup", command.commandName());

        assertEquals(1, command.commandHelpKeys(player).size());
        assertEquals("lookup", command.commandHelpKeys(player).get(0));
    }

    @Test
    void openLookupInventory() {
        // GIVEN
        Player player = mock(Player.class);
        String[] args = new String[]{"lookup", "BA"};

        // WHEN
        command.onCommand(player, args);

        // THEN
        verify(messages)
                .sendPending(player, "lookup");

        verify(inventoryManager)
                .openLookupInventory(player, "BA");
    }
}
