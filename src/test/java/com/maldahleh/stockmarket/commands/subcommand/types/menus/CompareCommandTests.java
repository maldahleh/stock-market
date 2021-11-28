package com.maldahleh.stockmarket.commands.subcommand.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.types.menus.CompareCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CompareCommandTests {

    private InventoryManager inventoryManager;
    private Messages messages;

    private CompareCommand command;

    @BeforeEach
    void setup() {
        inventoryManager = mock(InventoryManager.class);
        messages = mock(Messages.class);

        command = new CompareCommand(inventoryManager, messages);
    }

    @Test
    void hasRightProperties() {
        // GIVEN
        Player player = mock(Player.class);

        // THEN
        assertEquals(2, command.minArgs());
        assertEquals(2, command.maxArgs());
        assertEquals("stockmarket.compare", command.requiredPerm());
        assertEquals("compare", command.commandName());

        assertEquals(1, command.commandHelpKeys(player).size());
        assertEquals("compare", command.commandHelpKeys(player).get(0));
    }

    @Test
    void noSymbols() {
        // GIVEN
        Player player = mock(Player.class);
        String[] args = new String[]{"compare", "test"};

        // WHEN
        command.onCommand(player, args);

        // THEN
        verify(messages)
                .sendInvalidSyntax(player);
    }

    @Test
    void tooManySymbols() {
        // GIVEN
        Player player = mock(Player.class);
        String[] args = new String[]{"compare", "test,test2,test3,test4"};

        // WHEN
        command.onCommand(player, args);

        // THEN
        verify(messages)
                .sendCompareMax(player);
    }

    @Test
    void threeSymbols() {
        // GIVEN
        Player player = mock(Player.class);
        String[] args = new String[]{"compare", "test,test2,test3"};

        // WHEN
        command.onCommand(player, args);

        // THEN
        verify(messages)
                .sendPending(player, "compare");
        verify(inventoryManager)
                .openCompareInventory(player, "test", "test2", "test3");
    }
}
