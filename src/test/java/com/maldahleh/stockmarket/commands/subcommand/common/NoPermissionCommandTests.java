package com.maldahleh.stockmarket.commands.subcommand.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.maldahleh.stockmarket.commands.subcommands.common.NoPermissionCommand;
import com.maldahleh.stockmarket.config.Messages;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class NoPermissionCommandTests {

  @Test
  void hasRightProperties() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    NoPermissionCommand noPermissionCommand = new NoPermissionCommand(mock(Messages.class)) {
      @Override
      public void onCommand(Player player, String[] args) {
        // implementation not tested
      }

      @Override
      public String commandName() {
        return "test";
      }
    };

    // THEN
    assertEquals(1, noPermissionCommand.minArgs());
    assertEquals(1, noPermissionCommand.maxArgs());
    assertNull(noPermissionCommand.requiredPerm());

    assertEquals(1, noPermissionCommand.commandHelpKeys(player).size());
    assertEquals("test", noPermissionCommand.commandHelpKeys(player).get(0));
  }
}
