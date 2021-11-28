package com.maldahleh.stockmarket.commands.subcommand.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.types.menus.TutorialCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TutorialCommandTests {

    private InventoryManager inventoryManager;

    private TutorialCommand command;

    @BeforeEach
    void setup() {
        inventoryManager = mock(InventoryManager.class);

        command = new TutorialCommand(inventoryManager, mock(Messages.class));
    }

    @Test
    void hasRightProperties() {
        // GIVEN
        Player player = mock(Player.class);

        // THEN
        assertEquals(1, command.minArgs());
        assertEquals(1, command.maxArgs());
        assertEquals("stockmarket.tutorial", command.requiredPerm());
        assertEquals("tutorial", command.commandName());

        assertEquals(1, command.commandHelpKeys(player).size());
        assertEquals("tutorial", command.commandHelpKeys(player).get(0));
    }

    @Test
    void openInventory() {
        // GIVEN
        Player player = mock(Player.class);
        String[] args = new String[]{"tutorial"};

        // WHEN
        command.onCommand(player, args);

        // THEN
        verify(inventoryManager)
                .openTutorialInventory(player);
    }
}
