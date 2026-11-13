package com.maldahleh.stockmarket.bukkit;

import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

public class Messages {

  private volatile YamlConfiguration yaml;

  public Messages(YamlConfiguration yaml) {
    this.yaml = yaml;
  }

  public void reload(YamlConfiguration updated) {
    this.yaml = updated;
  }

  public String raw(String key) {
    return yaml.getString(key, key);
  }

  public List<String> lines(String key) {
    return yaml.isList(key) ? yaml.getStringList(key) : List.of(raw(key));
  }

  public void send(CommandSender to, String key) {
    send(to, key, Map.of());
  }

  public void send(CommandSender to, String key, Map<String, String> placeholders) {
    String prefix = yaml.getString("prefix", "");
    for (String line : lines(key)) {
      to.sendMessage(Text.render(prefix + line, placeholders));
    }
  }

  /** The message's lines with placeholders filled and colours applied. */
  public List<String> render(String key, Map<String, String> placeholders, boolean prefixed) {
    String prefix = prefixed ? yaml.getString("prefix", "") : "";
    return lines(key).stream().map(line -> Text.render(prefix + line, placeholders)).toList();
  }

  /** Lines without the prefix, e.g. multi-line summaries and help. */
  public void sendBlock(CommandSender to, String key, Map<String, String> placeholders) {
    for (String line : lines(key)) {
      to.sendMessage(Text.render(line, placeholders));
    }
  }
}
