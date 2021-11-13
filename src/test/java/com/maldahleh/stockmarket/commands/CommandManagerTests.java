package com.maldahleh.stockmarket.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.impl.StockMarketCommand;
import com.maldahleh.stockmarket.commands.impl.StockMarketTabCompleter;
import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.LookupCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import com.maldahleh.stockmarket.processor.types.SaleProcessor;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class CommandManagerTests {

  private PluginCommand pluginCommand;
  private CommandManager commandManager;

  @BeforeEach
  void setup() {
    this.pluginCommand = mock(PluginCommand.class);

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(() -> Bukkit.getPluginCommand("stockmarket"))
          .thenReturn(pluginCommand);

      this.commandManager = buildCommandManager();
    }
  }

  @Test
  void validatePostConstruct() {
    // WHEN setup called
    // THEN
    assertEquals(11, commandManager.getRegisteredSubcommands().size());

    verify(pluginCommand)
        .setExecutor(any(StockMarketCommand.class));

    verify(pluginCommand)
        .setTabCompleter(any(StockMarketTabCompleter.class));
  }

  @Test
  void nullPluginCommand() {
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      // GIVEN
      bukkit.when(() -> Bukkit.getPluginCommand("stockmarket"))
          .thenReturn(null);

      Logger logger = mock(Logger.class);
      bukkit.when(Bukkit::getLogger)
          .thenReturn(logger);

      // WHEN
      buildCommandManager();

      // THEN
      verify(logger)
          .severe("StockMarket - failed to find command");
    }
  }

  @Nested
  class BasePermission {

    @Test
    void hasPermission() {
      // GIVEN
      Player player = mock(Player.class);

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(true);

      // WHEN
      boolean doesNotHaveDefault = CommandManager.doesNotHaveBasePermission(player);

      // THEN
      assertFalse(doesNotHaveDefault);
    }

    @Test
    void noPermission() {
      // GIVEN
      Player player = mock(Player.class);

      when(player.hasPermission("stockmarket.use"))
          .thenReturn(false);

      // WHEN
      boolean doesNotHaveDefault = CommandManager.doesNotHaveBasePermission(player);

      // THEN
      assertTrue(doesNotHaveDefault);
    }
  }

  @Nested
  class FindSubcommand {

    @Test
    void foundSubcommand() {
      // GIVEN
      String subcommandName = "lookup";

      // WHEN
      Subcommand subcommand = commandManager.findSubcommand(subcommandName);

      // THEN
      assertTrue(subcommand instanceof LookupCommand);
    }

    @Test
    void nullSubcommand() {
      // GIVEN
      String subcommandName = "invalid";

      // WHEN
      Subcommand subcommand = commandManager.findSubcommand(subcommandName);

      // THEN
      assertNull(subcommand);
    }
  }

  private CommandManager buildCommandManager() {
    Plugin plugin = mock(Plugin.class);
    BrokerManager brokerManager = mock(BrokerManager.class);
    InventoryManager inventoryManager = mock(InventoryManager.class);
    PurchaseProcessor purchaseProcessor = mock(PurchaseProcessor.class);
    SaleProcessor saleProcessor = mock(SaleProcessor.class);
    Messages messages = mock(Messages.class);

    return new CommandManager(plugin, brokerManager, inventoryManager, purchaseProcessor,
        saleProcessor, messages);
  }
}
