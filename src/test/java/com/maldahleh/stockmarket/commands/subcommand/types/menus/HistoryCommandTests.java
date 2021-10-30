package com.maldahleh.stockmarket.commands.subcommand.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.types.menus.HistoryCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HistoryCommandTests {

    private InventoryManager inventoryManager;
    private Messages messages;

    private HistoryCommand command;

    @BeforeEach
    void setup() {
        inventoryManager = mock(InventoryManager.class);
        messages = mock(Messages.class);

        command = new HistoryCommand(inventoryManager, messages);
    }

    @Test
    void hasRightProperties() {
        // GIVEN
        Player player = mock(Player.class);

        // THEN
        assertEquals(1, command.minArgs());
        assertEquals(2, command.maxArgs());
        assertEquals("stockmarket.history", command.requiredPerm());
        assertEquals("history", command.commandName());

        List<String> helpKeys = command.commandHelpKeys(player);
        assertEquals(2, helpKeys.size());
        assertEquals("history", helpKeys.get(0));
        assertEquals("history-symbol", helpKeys.get(1));
    }

    @Test
    void openPlayerHistory() {
        // GIVEN
        Player player = mock(Player.class);
        String[] args = new String[]{"history"};

        // WHEN
        command.onCommand(player, args);

        // THEN
        verify(messages, times(1))
                .sendPendingHistory(player);

        verify(inventoryManager, times(1))
                .openStockHistoryInventory(player);
    }

    @Test
    void openStockHistory() {
        // GIVEN
        Player player = mock(Player.class);
        String[] args = new String[]{"history", "BA"};

        // WHEN
        command.onCommand(player, args);

        // THEN
        verify(messages, times(1))
                .sendPendingHistorySymbol(player);

        verify(inventoryManager, times(1))
                .openStockHistoryInventory(player, "BA");
    }
}
