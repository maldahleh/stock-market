package com.maldahleh.stockmarket.config.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.utils.Logger;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ConfigSectionTests {

  private static final String DUMMY_CONFIG = "config";

  private JavaPlugin javaPlugin;

  @BeforeEach
  void setup() {
    this.javaPlugin = mock(JavaPlugin.class);

    when(javaPlugin.getDataFolder())
        .thenReturn(new File("src/test/resources"));
  }

  @Test
  void locale() {
    // GIVEN
    ConfigSection section = new ConfigSection(javaPlugin, DUMMY_CONFIG);

    // WHEN
    Locale value = section.getLocale("locale");

    // THEN
    assertEquals(Locale.US, value);
  }

  @Test
  void validInt() {
    // GIVEN
    ConfigSection section = new ConfigSection(javaPlugin, DUMMY_CONFIG);

    // WHEN
    int value = section.getInt("transaction-cooldown-seconds");

    // THEN
    assertEquals(2, value);
  }

  @Test
  void invalidInt() {
    // GIVEN
    ConfigSection section = new ConfigSection(javaPlugin, DUMMY_CONFIG);

    // WHEN
    int value = section.getInt("transaction-cooldown-seconds-invalid");

    // THEN
    assertEquals(0, value);
  }

  @Test
  void validBoolean() {
    // GIVEN
    ConfigSection section = new ConfigSection(javaPlugin, DUMMY_CONFIG);

    // WHEN
    boolean value = section.getBoolean("sql.enabled");

    // THEN
    assertTrue(value);
  }

  @Test
  void invalidBoolean() {
    // GIVEN
    ConfigSection section = new ConfigSection(javaPlugin, DUMMY_CONFIG);

    // WHEN
    boolean value = section.getBoolean("sql.enabled-invalid");

    // THEN
    assertFalse(value);
  }

  @Test
  void stringList() {
    // GIVEN
    ConfigSection section = new ConfigSection(javaPlugin, DUMMY_CONFIG);

    // WHEN
    List<String> value = section.getStringList("allowed-currencies");

    // THEN
    assertEquals(3, value.size());
    assertTrue(value.containsAll(List.of("USD", "CAD")));
  }

  @Test
  void stringSet() {
    // GIVEN
    ConfigSection section = new ConfigSection(javaPlugin, DUMMY_CONFIG);

    // WHEN
    Set<String> value = section.getStringSet("allowed-currencies");

    // THEN
    assertEquals(2, value.size());
    assertTrue(value.containsAll(List.of("USD", "CAD")));
  }

  @Test
  void validBigDecimal() {
    // GIVEN
    ConfigSection section = new ConfigSection(javaPlugin, DUMMY_CONFIG);

    // WHEN
    BigDecimal value = section.getBigDecimal("minimum-price");

    // THEN
    assertEquals(BigDecimal.valueOf(5.0), value);
  }

  @Test
  void invalidBigDecimal() {
    // GIVEN
    ConfigSection section = new ConfigSection(javaPlugin, DUMMY_CONFIG);

    // WHEN
    BigDecimal value = section.getBigDecimal("minimum-price-invalid");

    // THEN
    assertEquals(BigDecimal.valueOf(0.0), value);
  }

  @Test
  void validConfigSection() {
    // GIVEN
    ConfigSection section = new ConfigSection(javaPlugin, DUMMY_CONFIG);

    // WHEN
    ConfigSection newSection = section.getConfigSection("sql");

    // THEN
    assertNotNull(newSection);
    assertTrue(newSection.getBoolean("enabled"));
  }

  @Test
  void invalidConfigSection() {
    // GIVEN
    ConfigSection section = new ConfigSection(javaPlugin, DUMMY_CONFIG);

    // WHEN
    ConfigSection newSection = section.getConfigSection("sql-invalid");

    // THEN
    assertNull(newSection);
  }

  @Test
  void notFoundFile() {
    // GIVEN
    String notFoundFileName = "config-missing";
    String fullFileName = notFoundFileName + ".yml";

    try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
      // WHEN
      new ConfigSection(javaPlugin, notFoundFileName);

      // THEN
      verify(javaPlugin)
          .saveResource(fullFileName, false);

      logger.verify(() -> Logger.severe("Failed to load " + fullFileName));
    }
  }
}
