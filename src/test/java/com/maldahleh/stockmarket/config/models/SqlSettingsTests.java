package com.maldahleh.stockmarket.config.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.config.common.ConfigSection;
import org.junit.jupiter.api.Test;

class SqlSettingsTests {

  @Test
  void propertiesSet() {
    // GIVEN
    ConfigSection configSection = mock(ConfigSection.class);

    when(configSection.getBoolean("enabled"))
        .thenReturn(true);

    when(configSection.getString("ip"))
        .thenReturn("127.0.0.1");

    when(configSection.getInt("port"))
        .thenReturn(3306);

    when(configSection.getString("database"))
        .thenReturn("stockmarket");

    when(configSection.getString("username"))
        .thenReturn("root");

    when(configSection.getString("password"))
        .thenReturn("password");

    // WHEN
    SqlSettings sqlSettings = new SqlSettings(configSection);

    // THEN
    assertTrue(sqlSettings.isEnabled());
    assertEquals("127.0.0.1", sqlSettings.getIp());
    assertEquals(3306, sqlSettings.getPort());
    assertEquals("stockmarket", sqlSettings.getDatabase());
    assertEquals("root", sqlSettings.getUsername());
    assertEquals("password", sqlSettings.getPassword());
  }
}
