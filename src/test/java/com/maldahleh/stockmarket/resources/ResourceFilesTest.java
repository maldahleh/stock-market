package com.maldahleh.stockmarket.resources;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maldahleh.stockmarket.orders.OrderBook;
import com.maldahleh.stockmarket.portfolio.EntryType;
import com.maldahleh.stockmarket.trade.TradeResult.Reason;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Catches typos in the shipped YAML: unknown materials and message keys the code can't find. */
class ResourceFilesTest {

  private static final List<String> MENUS = List.of("list.yml", "lookup.yml", "compare.yml",
      "portfolio.yml", "transactions.yml", "history.yml", "tutorial.yml");

  private static YamlConfiguration load(String name) throws IOException {
    try (var in = ResourceFilesTest.class.getClassLoader().getResourceAsStream(name)) {
      assertNotNull(in, name + " is missing");
      return YamlConfiguration.loadConfiguration(new InputStreamReader(in,
          StandardCharsets.UTF_8));
    }
  }

  @Test
  void everyMenuMaterialExists() throws IOException {
    List<String> unknown = new ArrayList<>();
    for (String menu : MENUS) {
      YamlConfiguration yaml = load(menu);
      for (String key : yaml.getKeys(true)) {
        if (key.endsWith(".material") && Material.matchMaterial(yaml.getString(key)) == null) {
          unknown.add(menu + " " + key + " = " + yaml.getString(key));
        }
      }
    }
    assertTrue(unknown.isEmpty(), "Unknown materials: " + unknown);
  }

  @Test
  void everyMessageKeyUsedInCodeExists() throws IOException {
    YamlConfiguration messages = load("messages.yml");
    Pattern literal = Pattern.compile(
        "(?:send|sendBlock|raw|render|lines|tell)\\((?:[^\"()]*,\\s*)?\"([a-z][a-z0-9.\\-]+)\"");
    List<String> missing = new ArrayList<>();
    try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
      for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
        Matcher matcher = literal.matcher(Files.readString(file));
        while (matcher.find()) {
          String key = matcher.group(1);
          if (!key.endsWith(".") && !messages.contains(key)) {
            missing.add(file.getFileName() + ": " + key);
          }
        }
      }
    }
    for (Reason reason : Reason.values()) {
      if (reason != Reason.PRICE_MOVED && !messages.contains("failures." + reason.name())) {
        missing.add("failures." + reason.name());
      }
    }
    for (OrderBook.CancelCause cause : OrderBook.CancelCause.values()) {
      if (!messages.contains("orders.causes." + cause.name())) {
        missing.add("orders.causes." + cause.name());
      }
    }
    for (OrderBook.Problem problem : OrderBook.Problem.values()) {
      if (!messages.contains("orders.problems." + problem.name())
          && !messages.contains("failures." + problem.name())) {
        missing.add("orders.problems." + problem.name());
      }
    }
    for (EntryType type : EntryType.values()) {
      if (!messages.contains("entry-types." + type.name())) {
        missing.add("entry-types." + type.name());
      }
    }
    assertTrue(missing.isEmpty(), "Missing message keys: " + missing);
  }
}
