package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.menu.Menus;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Broker villagers: they don't move or take damage, and right-clicking one opens the list. */
public class Brokers implements Listener {

  private final NamespacedKey key;
  private final String name;
  private final Menus menus;

  public Brokers(Plugin plugin, String name, Menus menus) {
    this.key = new NamespacedKey(plugin, "broker");
    this.name = name;
    this.menus = menus;
  }

  public void spawn(Location location) {
    location.getWorld().spawn(location, Villager.class, villager -> {
      villager.setCustomName(Text.color(name));
      villager.setCustomNameVisible(true);
      villager.setAI(false);
      villager.setInvulnerable(true);
      villager.setSilent(true);
      villager.setPersistent(true);
      villager.setRemoveWhenFarAway(false);
      villager.setCollidable(false);
      villager.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    });
  }

  private boolean isBroker(Entity entity) {
    return entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
  }

  @EventHandler(ignoreCancelled = true)
  public void onInteract(PlayerInteractEntityEvent event) {
    if (isBroker(event.getRightClicked())) {
      event.setCancelled(true);
      if (event.getPlayer().hasPermission("stockmarket.list")) {
        menus.openList(event.getPlayer());
      }
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    if (isBroker(event.getEntity())) {
      event.setCancelled(true);
    }
  }
}
