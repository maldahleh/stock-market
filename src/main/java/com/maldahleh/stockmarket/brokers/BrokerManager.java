package com.maldahleh.stockmarket.brokers;

import com.maldahleh.stockmarket.brokers.listeners.BrokerListener;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import com.maldahleh.stockmarket.utils.Utils;
import lombok.Getter;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BrokerManager {

  @Getter
  private final boolean enabled;

  @Getter
  private String simpleBrokerName;
  private boolean disableCommands;

  public BrokerManager(Plugin plugin, ConfigurationSection section,
      InventoryManager inventoryManager) {
    this.enabled = Bukkit.getPluginManager().isPluginEnabled("Citizens");
    if (!enabled) {
      return;
    }

    this.simpleBrokerName = Utils.color(section.getString("names.simple"));
    this.disableCommands = section.getBoolean("settings.disable-commands");

    plugin.getServer().getPluginManager().registerEvents(new BrokerListener(this,
        inventoryManager), plugin);
  }

  public void spawnSimpleBroker(Location location) {
    NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.VILLAGER, simpleBrokerName);
    npc.setProtected(true);
    npc.getDefaultGoalController().clear();
    npc.spawn(location, SpawnReason.CREATE);
  }

  public boolean isBroker(NPC npc) {
    if (npc == null || !npc.isSpawned()) {
      return false;
    }

    return npc.getName().equalsIgnoreCase(simpleBrokerName);
  }

  public boolean areCommandsDisabled(Player player) {
    if (!enabled || player.hasPermission("stockmarket.commandbypass")) {
      return false;
    }

    return disableCommands;
  }
}
