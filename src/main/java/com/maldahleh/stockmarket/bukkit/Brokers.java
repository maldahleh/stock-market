package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.menu.Menus;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import java.util.function.Supplier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Broker villagers: they don't move or take damage, and right-clicking one opens the list. */
public class Brokers implements Listener {

  private final NamespacedKey key;
  private final Supplier<Menus> menus;
  private final Messages messages;
  private volatile String name;

  /** @param menus null while the market isn't connected */
  public Brokers(Plugin plugin, String name, Supplier<Menus> menus, Messages messages) {
    this.key = new NamespacedKey(plugin, "broker");
    this.name = name;
    this.menus = menus;
    this.messages = messages;
  }

  public void rename(String newName) {
    this.name = newName;
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

  /**
   * Removes the broker nearest to {@code location}, within {@code radius} blocks.
   *
   * @return false if there is none that close
   */
  public boolean removeNearest(Location location, double radius) {
    Entity nearest = null;
    double best = Double.MAX_VALUE;
    for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius,
        radius)) {
      double distance = entity.getLocation().distanceSquared(location);
      if (isBroker(entity) && distance < best) {
        nearest = entity;
        best = distance;
      }
    }
    if (nearest == null) {
      return false;
    }
    nearest.remove();
    return true;
  }

  private boolean isBroker(Entity entity) {
    return entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
  }

  @EventHandler(ignoreCancelled = true)
  public void onInteract(PlayerInteractEntityEvent event) {
    if (!isBroker(event.getRightClicked())) {
      return;
    }
    event.setCancelled(true);
    // Spigot fires this once per hand; answer only the main hand.
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }
    Player player = event.getPlayer();
    Menus current = menus.get();
    if (current == null) {
      messages.send(player, "not-configured");
    } else if (!player.hasPermission("stockmarket.list")) {
      messages.send(player, "no-permission");
    } else {
      current.openList(player);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    if (isBroker(event.getEntity())) {
      event.setCancelled(true);
    }
  }
}
