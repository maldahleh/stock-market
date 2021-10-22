package com.maldahleh.stockmarket.commands.subcommand.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.maldahleh.stockmarket.commands.subcommands.common.BaseCommand;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class BaseCommandTests {

  @Test
  void baseCommandHasRightProperties() {
    // GIVEN
    Player player = mock(Player.class);
    BaseCommand baseCommand = new BaseCommand() {
      @Override
      public void onCommand(Player player, String[] args) {

      }

      @Override
      public String commandName() {
        return "test";
      }
    };

    // VERIFY
    assertEquals(1, baseCommand.minArgs());
    assertEquals(1, baseCommand.maxArgs());
    assertEquals("stockmarket.test", baseCommand.requiredPerm());

    assertEquals(1, baseCommand.commandHelpKeys(player).size());
    assertEquals("test", baseCommand.commandHelpKeys(player).get(0));
  }
}
