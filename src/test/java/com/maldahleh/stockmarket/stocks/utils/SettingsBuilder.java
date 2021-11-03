package com.maldahleh.stockmarket.stocks.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.config.Settings;

public class SettingsBuilder {

  public static Settings buildSettings() {
    return buildSettings(1);
  }

  public static Settings buildSettings(int cacheMinutes) {
    Settings settings = mock(Settings.class);

    when(settings.getCacheMinutes())
        .thenReturn(cacheMinutes);

    return settings;
  }
}
