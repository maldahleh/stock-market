package com.maldahleh.stockmarket.commands.subcommand.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import com.maldahleh.stockmarket.config.Messages;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BaseCommandTests {

  @Test
  void hasRightProperties() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    BaseCommand baseCommand = new BaseCommand(mock(Messages.class)) {
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
    assertEquals(1, baseCommand.minArgs());
    assertEquals(1, baseCommand.maxArgs());
    assertEquals("stockmarket.test", baseCommand.requiredPerm());

    assertEquals(1, baseCommand.commandHelpKeys(player).size());
    assertEquals("test", baseCommand.commandHelpKeys(player).get(0));

    assertFalse(baseCommand.shouldTabCompleterReturnPlayerList(player));
  }

  @Nested
  @DisplayName("canPlayerExecute")
  class CanPlayerExecute {

    @Test
    void hasPermission() {
      // GIVEN
      Player player = mock(Player.class);
      BaseCommand baseCommand = new BaseCommand(mock(Messages.class)) {
        @Override
        public void onCommand(Player player, String[] args) {
          // implementation not tested
        }

        @Override
        public String commandName() {
          return "test";
        }
      };

      when(player.hasPermission("stockmarket.test"))
          .thenReturn(true);

      // WHEN
      boolean canExecute = baseCommand.canPlayerExecute(player);

      // THEN
      assertTrue(canExecute);
    }

    @Test
    void noPermissionCommand() {
      // GIVEN
      Player player = mock(Player.class);
      BaseCommand baseCommand = new BaseCommand(mock(Messages.class)) {
        @Override
        public void onCommand(Player player, String[] args) {
          // implementation not tested
        }

        @Override
        public String commandName() {
          return "test";
        }

        @Override
        public String requiredPerm() {
          return null;
        }
      };

      // WHEN
      boolean canExecute = baseCommand.canPlayerExecute(player);

      // THEN
      assertTrue(canExecute);
    }
  }
}
