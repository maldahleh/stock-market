package com.maldahleh.stockmarket.utils;

import java.time.Duration;
import java.time.Instant;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeUtils {

  public long minutesSince(Instant timestamp) {
    return Duration.between(timestamp, Instant.now()).toMinutes();
  }
}
