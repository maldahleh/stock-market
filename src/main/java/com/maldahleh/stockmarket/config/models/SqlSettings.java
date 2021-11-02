package com.maldahleh.stockmarket.config.models;

import com.maldahleh.stockmarket.config.common.ConfigSection;
import lombok.Getter;

@Getter
public class SqlSettings {

  private final boolean enabled;
  private final int ip;
  private final int port;
  private final String database;
  private final String username;
  private final String password;
  private final int maxPoolSize;

  public SqlSettings(ConfigSection configSection) {
    this.enabled = configSection.getBoolean("enabled");
    this.ip = configSection.getInt("ip");
    this.port = configSection.getInt("port");
    this.database = configSection.getString("database");
    this.username = configSection.getString("username");
    this.password = configSection.getString("password");
    this.maxPoolSize = configSection.getInt("max-pool-size");
  }
}
