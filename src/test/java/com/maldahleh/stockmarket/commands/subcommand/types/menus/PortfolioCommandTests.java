package com.maldahleh.stockmarket.commands.subcommand.types.menus;

import com.maldahleh.stockmarket.commands.subcommands.types.menus.PortfolioCommand;
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

class PortfolioCommandTests {

    private InventoryManager inventoryManager;
    private Messages messages;

    private PortfolioCommand command;

    @BeforeEach
    void setup() {
        Plugin plugin = mock(Plugin.class);
        inventoryManager = mock(InventoryManager.class);
        messages = mock(Messages.class);

        command = new PortfolioCommand(plugin, inventoryManager, messages);
    }

    @Test
    void hasRightProperties() {
        // GIVEN
        Player player = mock(Player.class);

        when(player.hasPermission("stockmarket.portfolio.other"))
                .thenReturn(false);

        // THEN
        assertEquals(1, command.minArgs());
        assertEquals(2, command.maxArgs());
        assertEquals("stockmarket.portfolio", command.requiredPerm());
        assertEquals("portfolio", command.commandName());

        assertEquals(1, command.commandHelpKeys(player).size());
        assertEquals("portfolio", command.commandHelpKeys(player).get(0));
    }

    @Test
    void hasRightProperties_playerHasOtherPerm() {
        // GIVEN
        Player player = mock(Player.class);

        when(player.hasPermission("stockmarket.portfolio.other"))
                .thenReturn(true);

        // THEN
        assertEquals(1, command.minArgs());
        assertEquals(2, command.maxArgs());
        assertEquals("stockmarket.portfolio", command.requiredPerm());
        assertEquals("portfolio", command.commandName());

        List<String> commandHelp = command.commandHelpKeys(player);
        assertEquals(2, commandHelp.size());
        assertEquals("portfolio", commandHelp.get(0));
        assertEquals("portfolio-other", commandHelp.get(1));
    }

    @Test
    void callerAction() {
        // GIVEN
        Player player = mock(Player.class);

        // WHEN
        command.callerAction(player);

        // THEN
        verify(messages, times(1))
                .sendPendingPortfolio(player);

        verify(inventoryManager, times(1))
                .openPortfolioInventory(player);
    }

    @Test
    void targetAction() {
        // GIVEN
        Player player = mock(Player.class);
        UUID targetUUID = UUID.randomUUID();

        // WHEN
        command.targetAction(player, targetUUID);

        // THEN
        verify(messages, times(1))
                .sendPendingPortfolioOther(player);

        verify(inventoryManager, times(1))
                .openPortfolioInventory(player, targetUUID);
    }
}
