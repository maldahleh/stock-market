package com.maldahleh.stockmarket.brokers;

import com.maldahleh.stockmarket.brokers.listeners.BrokerListener;
import com.maldahleh.stockmarket.commands.CommandManager;
import com.maldahleh.stockmarket.config.models.BrokerSettings;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import lombok.Getter;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BrokerManager {

  @Getter
  private final boolean enabled = Bukkit.getPluginManager().isPluginEnabled("Citizens");
  private final BrokerSettings settings;

  public BrokerManager(Plugin plugin, BrokerSettings settings, InventoryManager inventoryManager) {
    this.settings = settings;
    if (!enabled) {
      return;
    }

    plugin
        .getServer()
        .getPluginManager()
        .registerEvents(new BrokerListener(this, inventoryManager), plugin);
  }

  public void spawnSimpleBroker(Location location) {
    NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.VILLAGER, getSimpleBrokerName());
    npc.setProtected(true);
    npc.getDefaultGoalController().clear();
    npc.spawn(location, SpawnReason.CREATE);
  }

  public boolean isBroker(NPC npc) {
    if (npc == null || !npc.isSpawned()) {
      return false;
    }

    return npc.getName().equalsIgnoreCase(getSimpleBrokerName());
  }

  public boolean areCommandsDisabled(Player player) {
    if (!enabled || player.hasPermission(CommandManager.COMMAND_BYPASS_PERM)) {
      return false;
    }

    return settings.isCommandsDisabled();
  }

  private String getSimpleBrokerName() {
    return settings.getSimpleBrokerName();
  }
}
