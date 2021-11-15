package com.maldahleh.stockmarket.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class LoggerTests {

  @Test
  void severe() {
    // GIVEN
    String message = "severe message";

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      Logger logger = mock(Logger.class);

      bukkit.when(Bukkit::getLogger)
          .thenReturn(logger);

      // WHEN
      com.maldahleh.stockmarket.utils.Logger.severe(message);

      // THEN
      verify(logger)
          .severe("StockMarket - severe message");
    }
  }
}
