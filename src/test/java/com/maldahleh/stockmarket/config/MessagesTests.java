package com.maldahleh.stockmarket.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.StockMarket;
import java.io.File;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessagesTests {

  private Settings settings;
  private Messages messages;

  @BeforeEach
  void setup() {
    this.settings = mock(Settings.class);
    this.messages = new Messages(mockedStockMarket(), settings);
  }

  @Test
  void commandsDisabled() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    messages.sendCommandsDisabled(player);

    // THEN
    verify(player)
        .sendMessage(color("&cCommands are disabled, please use the stock brokers."));
  }

  private String color(String message) {
    return ChatColor.translateAlternateColorCodes('&', message);
  }

  private StockMarket mockedStockMarket() {
    StockMarket stockMarket = mock(StockMarket.class);

    when(stockMarket.getDataFolder())
        .thenReturn(new File("src/test/resources"));

    return stockMarket;
  }
}
