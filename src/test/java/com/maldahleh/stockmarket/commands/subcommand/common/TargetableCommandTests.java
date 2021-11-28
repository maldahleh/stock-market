package com.maldahleh.stockmarket.commands.subcommand.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.commands.subcommands.common.TargetableCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import com.maldahleh.stockmarket.utils.SchedulerUtils;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class TargetableCommandTests {

  private Plugin plugin;
  private Messages messages;
  private TargetableCommand targetableCommand;

  @BeforeEach
  void setup() {
    InventoryManager inventoryManager = mock(InventoryManager.class);

    this.plugin = mock(Plugin.class);
    this.messages = mock(Messages.class);
    this.targetableCommand = spy(
        new TargetableCommand(plugin, inventoryManager, messages) {
          @Override
          public void callerAction(Player caller) {
            // implementation not tested
          }

          @Override
          public void targetAction(Player caller, UUID target) {
            // implementation not tested
          }

          @Override
          public String commandName() {
            return "test";
          }
        }
    );
  }

  @Test
  void hasRightProperties() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    when(player.hasPermission("stockmarket.test.other"))
        .thenReturn(false);

    // THEN
    assertEquals(1, targetableCommand.minArgs());
    assertEquals(2, targetableCommand.maxArgs());
    assertEquals("stockmarket.test", targetableCommand.requiredPerm());

    assertEquals(1, targetableCommand.commandHelpKeys(player).size());
    assertEquals("test", targetableCommand.commandHelpKeys(player).get(0));
  }

  @Test
  void hasRightPropertiesOther() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    when(player.hasPermission("stockmarket.test.other"))
        .thenReturn(true);

    // THEN
    assertEquals(1, targetableCommand.minArgs());
    assertEquals(2, targetableCommand.maxArgs());
    assertEquals("stockmarket.test", targetableCommand.requiredPerm());

    assertEquals(2, targetableCommand.commandHelpKeys(player).size());
    assertEquals("test", targetableCommand.commandHelpKeys(player).get(0));
    assertEquals("test-other", targetableCommand.commandHelpKeys(player).get(1));
  }

  @Nested
  @DisplayName("onCommand")
  class OnCommand {

    @Test
    void oneArg() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[]{"test"};

      // WHEN
      targetableCommand.onCommand(player, args);

      // THEN
      verify(messages)
          .sendPending(player, "test");

      verify(targetableCommand)
          .callerAction(player);
    }

    @Nested
    @DisplayName("twoArgs")
    class TwoArgs {

      @Test
      void noPermission() {
        // GIVEN
        Player player = mock(Player.class);
        String[] args = new String[]{"test", "target"};

        when(player.hasPermission("stockmarket.test.other"))
            .thenReturn(false);

        // WHEN
        targetableCommand.onCommand(player, args);

        // THEN
        verify(messages)
            .sendNoPermission(player);
      }
    }

    @Test
    void targetOnline() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[]{"test", "target"};

      when(player.hasPermission("stockmarket.test.other"))
          .thenReturn(true);

      try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
        Player target = mock(Player.class);

        UUID uuid = UUID.randomUUID();
        when(target.getUniqueId())
            .thenReturn(uuid);

        bukkit.when(() -> Bukkit.getPlayer("target"))
            .thenReturn(target);

        // WHEN
        targetableCommand.onCommand(player, args);

        // THEN
        verify(messages)
            .sendPending(player, "test-other");

        verify(targetableCommand)
            .targetAction(player, uuid);
      }
    }

    @Test
    @SuppressWarnings("deprecation")
    void targetOffline() {
      // GIVEN
      Player player = mock(Player.class);
      String[] args = new String[]{"test", "target"};

      when(player.hasPermission("stockmarket.test.other"))
          .thenReturn(true);

      try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
        Player target = mock(Player.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);

        UUID uuid = UUID.randomUUID();
        when(target.getUniqueId())
            .thenReturn(uuid);

        bukkit.when(() -> Bukkit.getPlayer("target"))
            .thenReturn(null);

        bukkit.when(() -> Bukkit.getOfflinePlayer("target"))
            .thenReturn(target);

        bukkit.when(Bukkit::getScheduler)
            .thenReturn(scheduler);

        // WHEN
        targetableCommand.onCommand(player, args);

        SchedulerUtils.interceptAsyncRun(plugin, scheduler);

        // THEN
        verify(messages)
            .sendPending(player, "test-other");

        verify(targetableCommand)
            .targetAction(player, uuid);
      }
    }
  }
}
