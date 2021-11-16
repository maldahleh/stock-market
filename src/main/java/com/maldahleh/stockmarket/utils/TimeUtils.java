package com.maldahleh.stockmarket.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeUtils {

  public String formatInstant(Instant instant, Locale locale) {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())
        .format(instant);
  }
}
