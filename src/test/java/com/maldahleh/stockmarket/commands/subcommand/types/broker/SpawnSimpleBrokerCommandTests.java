package com.maldahleh.stockmarket.commands.subcommand.types.broker;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.subcommands.types.broker.SpawnSimpleBrokerCommand;
import com.maldahleh.stockmarket.config.Messages;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SpawnSimpleBrokerCommandTests {

    private BrokerManager brokerManager;
    private Messages messages;

    private SpawnSimpleBrokerCommand command;

    @BeforeEach
    void setup() {
        this.brokerManager = mock(BrokerManager.class);
        this.messages = mock(Messages.class);

        this.command = new SpawnSimpleBrokerCommand(brokerManager, messages);
    }

    @Test
    void hasRightProperties() {
        // GIVEN
        Player player = mock(Player.class);

        // THEN
        assertEquals(1, command.minArgs());
        assertEquals(1, command.maxArgs());
        assertEquals("stockmarket.simplebroker", command.requiredPerm());
        assertEquals("simplebroker", command.commandName());

        assertEquals(1, command.commandHelpKeys(player).size());
        assertEquals("simplebroker", command.commandHelpKeys(player).get(0));
    }

    @Test
    void brokersDisabled() {
        // GIVEN
        Player player = mock(Player.class);

        when(brokerManager.isEnabled())
                .thenReturn(false);

        // WHEN
        command.onCommand(player, new String[]{});

        // THEN
        verify(messages)
            .sendCitizensRequired(player);
    }

    @Test
    void brokersEnabled() {
        // GIVEN
        Player player = mock(Player.class);
        Location location = mock(Location.class);

        when(brokerManager.isEnabled())
                .thenReturn(true);

        when(player.getLocation())
                .thenReturn(location);

        // WHEN
        command.onCommand(player, new String[]{});

        // THEN
        verify(brokerManager)
                .spawnSimpleBroker(location);
    }
}
