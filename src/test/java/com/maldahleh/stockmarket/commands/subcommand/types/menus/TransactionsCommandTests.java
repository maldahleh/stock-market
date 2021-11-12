package com.maldahleh.stockmarket.commands.subcommand.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.types.menus.TransactionsCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TransactionsCommandTests {

    private InventoryManager inventoryManager;
    private Messages messages;

    private TransactionsCommand command;

    @BeforeEach
    void setup() {
        Plugin plugin = mock(Plugin.class);
        inventoryManager = mock(InventoryManager.class);
        messages = mock(Messages.class);

        command = new TransactionsCommand(plugin, inventoryManager, messages);
    }

    @Test
    void hasRightProperties() {
        // GIVEN
        Player player = mock(Player.class);

        when(player.hasPermission("stockmarket.transactions.other"))
                .thenReturn(false);

        // THEN
        assertEquals(1, command.minArgs());
        assertEquals(2, command.maxArgs());
        assertEquals("stockmarket.transactions", command.requiredPerm());
        assertEquals("transactions", command.commandName());

        assertEquals(1, command.commandHelpKeys(player).size());
        assertEquals("transactions", command.commandHelpKeys(player).get(0));
    }

    @Test
    void hasRightPropertiesPlayerHasOtherPerm() {
        // GIVEN
        Player player = mock(Player.class);

        when(player.hasPermission("stockmarket.transactions.other"))
                .thenReturn(true);

        // THEN
        assertEquals(1, command.minArgs());
        assertEquals(2, command.maxArgs());
        assertEquals("stockmarket.transactions", command.requiredPerm());
        assertEquals("transactions", command.commandName());

        List<String> commandHelp = command.commandHelpKeys(player);
        assertEquals(2, commandHelp.size());
        assertEquals("transactions", commandHelp.get(0));
        assertEquals("transactions-other", commandHelp.get(1));
    }

    @Test
    void callerAction() {
        // GIVEN
        Player player = mock(Player.class);

        // WHEN
        command.callerAction(player);

        // THEN
        verify(messages)
                .sendPendingTransactions(player);

        verify(inventoryManager)
                .openTransactionInventory(player);
    }

    @Test
    void targetAction() {
        // GIVEN
        Player player = mock(Player.class);
        UUID targetUUID = UUID.randomUUID();

        // WHEN
        command.targetAction(player, targetUUID);

        // THEN
        verify(messages)
                .sendPendingTransactionsOther(player);

        verify(inventoryManager)
                .openTransactionInventory(player, targetUUID);
    }
}
