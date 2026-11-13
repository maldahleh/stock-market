package com.maldahleh.stockmarket.bukkit;

import java.time.Instant;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Remembers the plugin's last warning or error and whether the data source answered, for
 * {@code /stocks status}. Attach it to the plugin's logger.
 */
public class Health extends Handler {

  private volatile String lastProblem;
  private volatile Instant lastProblemAt;
  private volatile Boolean connected;
  private volatile String connection = "not checked yet";

  @Override
  public void publish(LogRecord record) {
    if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
      Throwable error = record.getThrown();
      lastProblem = error == null || error.getMessage() == null ? record.getMessage()
          : record.getMessage() + " (" + error.getMessage() + ")";
      lastProblemAt = Instant.ofEpochMilli(record.getMillis());
    }
  }

  public void connected(boolean ok, String detail) {
    this.connected = ok;
    this.connection = detail;
  }

  /** True if the last check reached the data source, false if it failed, null before one. */
  public Boolean connected() {
    return connected;
  }

  public String connection() {
    return connection;
  }

  public String lastProblem() {
    return lastProblem;
  }

  public Instant lastProblemAt() {
    return lastProblemAt;
  }

  @Override
  public void flush() {
    // Nothing is buffered.
  }

  @Override
  public void close() {
    // Nothing to release.
  }
}
