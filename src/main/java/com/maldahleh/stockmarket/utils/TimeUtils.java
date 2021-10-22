package com.maldahleh.stockmarket.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeUtils {

  public long minutesSince(Instant timestamp) {
    return Duration.between(timestamp, Instant.now()).toMinutes();
  }

  public String getCurrentTime() {
    SimpleDateFormat dateFormat = new SimpleDateFormat();
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    return dateFormat.format(new Date());
  }

  public String formatDate(Date date, Locale locale) {
    DateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", locale);
    return dateFormat.format(date);
  }

  public String formatInstant(Instant instant, Locale locale) {
    return DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())
        .format(instant);
  }
}
