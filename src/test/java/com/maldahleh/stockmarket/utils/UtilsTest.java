package com.maldahleh.stockmarket.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;

class UtilsTest {

  @Test
  void color() {
    // GIVEN
    String message = "&cMessage";

    // WHEN
    String colored = Utils.color(message);

    // THEN
    assertEquals(ChatColor.translateAlternateColorCodes('&', "&cMessage"), colored);
  }

  @Test
  void createItemStackNoReplacement() {
    // GIVEN
    ConfigurationSection section = mock(ConfigurationSection.class);

    when(section.get("material"))
        .thenReturn("PLANKS");

    when(section.get("amount"))
        .thenReturn(2);
  }
}
