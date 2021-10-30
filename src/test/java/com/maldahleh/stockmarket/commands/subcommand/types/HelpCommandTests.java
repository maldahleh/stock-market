package com.maldahleh.stockmarket.commands.subcommand.types;

import com.maldahleh.stockmarket.commands.subcommands.types.HelpCommand;
import com.maldahleh.stockmarket.config.Messages;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class HelpCommandTests {

    private Messages messages;

    private HelpCommand command;

    @BeforeEach
    void setup() {
        messages = mock(Messages.class);

        command = new HelpCommand(messages);
    }

    @Test
    void hasRightProperties() {
        // GIVEN
        Player player = mock(Player.class);

        // THEN
        assertEquals(1, command.minArgs());
        assertEquals(1, command.maxArgs());
        assertEquals("help", command.commandName());
        assertNull(command.requiredPerm());

        assertEquals(1, command.commandHelpKeys(player).size());
        assertEquals("help", command.commandHelpKeys(player).get(0));
    }

    @Test
    void sendHelp() {
        // GIVEN
        Player player = mock(Player.class);

        // WHEN
        command.onCommand(player, new String[]{});

        // THEN
        verify(messages, times(1))
                .sendHelpMessage(player);
    }
}
