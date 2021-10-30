package com.maldahleh.stockmarket.commands.subcommand.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.types.menus.ListCommand;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ListCommandTests {

    private InventoryManager inventoryManager;

    private ListCommand command;

    @BeforeEach
    void setup() {
        inventoryManager = mock(InventoryManager.class);

        command = new ListCommand(inventoryManager);
    }

    @Test
    void hasRightProperties() {
        // GIVEN
        Player player = mock(Player.class);

        // THEN
        assertEquals(1, command.minArgs());
        assertEquals(1, command.maxArgs());
        assertEquals("stockmarket.list", command.requiredPerm());
        assertEquals("list", command.commandName());

        assertEquals(1, command.commandHelpKeys(player).size());
        assertEquals("list", command.commandHelpKeys(player).get(0));
    }

    @Test
    void openListInventory() {
        // GIVEN
        Player player = mock(Player.class);
        String[] args = new String[]{"list"};

        // WHEN
        command.onCommand(player, args);

        // THEN
        verify(inventoryManager, times(1))
                .openListInventory(player);
    }
}
