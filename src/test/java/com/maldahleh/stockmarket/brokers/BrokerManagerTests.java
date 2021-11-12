package com.maldahleh.stockmarket.brokers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.commands.CommandManager;
import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.config.models.BrokerSettings;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.GoalController;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class BrokerManagerTests {

  private Plugin plugin;
  private PluginManager pluginManager;
  private ConfigSection configurationSection;
  private InventoryManager inventoryManager;

  private BrokerManager brokerManager;

  @BeforeEach
  void setUpMocks() {
    plugin = mock(Plugin.class);
    pluginManager = mock(PluginManager.class);
    configurationSection = mock(ConfigSection.class);
    inventoryManager = mock(InventoryManager.class);

    Server server = mock(Server.class);

    when(configurationSection.getString("names.simple"))
        .thenReturn("broker");

    when(configurationSection.getBoolean("disable-commands"))
        .thenReturn(false);

    when(plugin.getServer())
        .thenReturn(server);

    when(server.getPluginManager())
        .thenReturn(pluginManager);

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager)
          .thenReturn(pluginManager);

      when(pluginManager.isPluginEnabled("Citizens"))
          .thenReturn(true);

      BrokerSettings brokerSettings = new BrokerSettings(configurationSection);
      brokerManager = new BrokerManager(plugin, brokerSettings, inventoryManager);
    }
  }

  @Test
  void citizensNotEnabled() {
    // GIVEN
    reset(configurationSection);

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager)
          .thenReturn(pluginManager);

      when(pluginManager.isPluginEnabled("Citizens"))
          .thenReturn(false);

      BrokerSettings brokerSettings = new BrokerSettings(configurationSection);

      // WHEN
      BrokerManager brokerManager = new BrokerManager(plugin, brokerSettings, inventoryManager);

      // THEN
      assertFalse(brokerManager.isEnabled());
    }
  }

  @Test
  void spawnSimpleBroker() {
    // GIVEN
    NPC npc = mock(NPC.class);
    Location location = mock(Location.class);
    NPCRegistry registry = mock(NPCRegistry.class);
    GoalController goalController = mock(GoalController.class);

    try (MockedStatic<CitizensAPI> citizensAPI = mockStatic(CitizensAPI.class)) {
      citizensAPI.when(CitizensAPI::getNPCRegistry)
          .thenReturn(registry);

      when(registry.createNPC(EntityType.VILLAGER, "broker"))
          .thenReturn(npc);

      when(npc.getDefaultGoalController())
          .thenReturn(goalController);

      // WHEN
      brokerManager.spawnSimpleBroker(location);

      // THEN
      verify(npc)
          .setProtected(true);

      verify(npc)
          .spawn(location, SpawnReason.CREATE);

      verify(goalController)
          .clear();
    }
  }

  @Nested
  @DisplayName("areCommandsDisabled")
  class AreCommandsDisabled {

    private Player player;

    @BeforeEach
    void mockPlayer() {
      player = mock(Player.class);
    }

    @Test
    void playerNoPermission() {
      // GIVEN
      when(player.hasPermission(CommandManager.COMMAND_BYPASS_PERM))
          .thenReturn(false);

      when(configurationSection.getBoolean("disable-commands"))
          .thenReturn(true);

      // WHEN
      boolean result = brokerManager.areCommandsDisabled(player);

      // THEN
      assertTrue(result);
    }

    @Test
    void playerNoPermissionCommandsEnabled() {
      // GIVEN
      when(player.hasPermission(CommandManager.COMMAND_BYPASS_PERM))
          .thenReturn(false);

      when(configurationSection.getBoolean("disable-commands"))
          .thenReturn(false);

      // WHEN
      boolean result = brokerManager.areCommandsDisabled(player);

      // THEN
      assertFalse(result);
    }

    @Test
    void playerHasPermissionCommandsDisabled() {
      // GIVEN
      when(player.hasPermission(CommandManager.COMMAND_BYPASS_PERM))
          .thenReturn(true);

      when(configurationSection.getBoolean("disable-commands"))
          .thenReturn(true);

      // WHEN
      boolean result = brokerManager.areCommandsDisabled(player);

      // THEN
      assertFalse(result);
    }
  }

  @Nested
  @DisplayName("isBroker")
  class IsBroker {

    @Test
    void nullNpc() {
      // WHEN
      boolean result = brokerManager.isBroker(null);

      // THEN
      assertFalse(result);
    }

    @Test
    void notSpawnedNpc() {
      // GIVEN
      NPC npc = mock(NPC.class);

      when(npc.isSpawned())
          .thenReturn(false);

      // WHEN
      boolean result = brokerManager.isBroker(npc);

      // THEN
      assertFalse(result);
    }

    @Test
    void spawnedNpc() {
      // GIVEN
      NPC npc = mock(NPC.class);

      when(npc.isSpawned())
          .thenReturn(true);

      when(npc.getName())
          .thenReturn("broker");

      // WHEN
      boolean result = brokerManager.isBroker(npc);

      // THEN
      assertTrue(result);
    }
  }
}
