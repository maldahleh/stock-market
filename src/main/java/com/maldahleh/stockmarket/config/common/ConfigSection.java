package com.maldahleh.stockmarket.config.common;

import com.maldahleh.stockmarket.utils.Logger;
import com.maldahleh.stockmarket.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public class ConfigSection {

  private final ConfigurationSection config;

  public ConfigSection(JavaPlugin plugin, String fileName) {
    this.config = loadFile(plugin, fileName);
  }

  public ConfigSection getConfigSection(String path) {
    ConfigurationSection section = config.getConfigurationSection(path);
    if (section == null) {
      return null;
    }

    return new ConfigSection(config.getConfigurationSection(path));
  }

  public String getString(String path) {
    String string = config.getString(path, "");
    return Utils.color(string);
  }

  public int getInt(String path) {
    return config.getInt(path, 0);
  }

  public boolean getBoolean(String path) {
    return config.getBoolean(path, false);
  }

  public List<String> getStringList(String path) {
    return config.getStringList(path);
  }

  public Set<String> getStringSet(String path) {
    List<String> stringList = getStringList(path);
    return new HashSet<>(stringList);
  }

  public BigDecimal getBigDecimal(String path) {
    double value = config.getDouble(path, 0);
    return BigDecimal.valueOf(value);
  }

  public Locale getLocale(String path) {
    String value = getString(path);
    return Locale.forLanguageTag(value);
  }

  private ConfigurationSection loadFile(JavaPlugin plugin, String fileName) {
    String fullFileName = fileName + ".yml";
    File configFile = new File(plugin.getDataFolder(), fullFileName);
    if (!configFile.exists()) {
      configFile.getParentFile().mkdirs();
      plugin.saveResource(fullFileName, false);
    }

    YamlConfiguration yamlConfig = new YamlConfiguration();
    try {
      yamlConfig.load(configFile);
      return yamlConfig;
    } catch (IOException | InvalidConfigurationException e) {
      Logger.severe("Failed to load " + fullFileName);
      e.printStackTrace();
      return null;
    }
  }
}
