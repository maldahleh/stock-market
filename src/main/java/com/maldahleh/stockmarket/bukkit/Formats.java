package com.maldahleh.stockmarket.bukkit;

import com.maldahleh.stockmarket.market.Quantities;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.milkbowl.vault.economy.Economy;

/** Number, money and date formatting in the server's chosen locale. */
public class Formats {

  private final Economy economy;
  private final Locale locale;
  private final DateTimeFormatter dates;
  private final String unknown;

  public Formats(Economy economy, Locale locale, String datePattern, String unknown) {
    this.economy = economy;
    this.locale = locale;
    this.dates = DateTimeFormatter.ofPattern(datePattern, locale).withZone(ZoneId.systemDefault());
    this.unknown = unknown;
  }

  public String unknown() {
    return unknown;
  }

  /** Server money, formatted by the economy plugin. */
  public String money(BigDecimal amount) {
    return amount == null ? unknown : economy.format(amount.doubleValue());
  }

  /** A market price in US dollars. */
  public String usd(BigDecimal amount) {
    if (amount == null) {
      return unknown;
    }
    NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
    format.setMaximumFractionDigits(amount.abs().compareTo(BigDecimal.ONE) < 0 ? 6 : 2);
    return format.format(amount);
  }

  /** Shares as a whole number, or a coin amount such as 0.015 for crypto. */
  public String quantity(String symbol, long units) {
    if (Quantities.unitSize(symbol).compareTo(BigDecimal.ONE) == 0) {
      return number(units);
    }
    return Quantities.amount(symbol, units).toPlainString();
  }

  public String number(long value) {
    return NumberFormat.getIntegerInstance(locale).format(value);
  }

  /** e.g. 1.2M */
  public String compact(long value) {
    return NumberFormat.getCompactNumberInstance(locale, NumberFormat.Style.SHORT).format(value);
  }

  /** A signed percent, e.g. +1.25%. */
  public String percent(BigDecimal value) {
    if (value == null) {
      return unknown;
    }
    BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP);
    return (rounded.signum() > 0 ? "+" : "") + rounded.toPlainString() + "%";
  }

  /** Signed server money, e.g. +$12.00 or -$3.50. */
  public String signedMoney(BigDecimal amount) {
    if (amount == null) {
      return unknown;
    }
    String formatted = money(amount.abs());
    return amount.signum() < 0 ? "-" + formatted : "+" + formatted;
  }

  /** Time from now until {@code instant}, e.g. "in 2h 15m" or "in 2d 4h". */
  public String until(Instant instant) {
    long minutes = Math.max(1, java.time.Duration.between(Instant.now(), instant).toMinutes());
    long days = minutes / (60 * 24);
    long hours = minutes / 60 % 24;
    if (days > 0) {
      return "in " + days + "d " + hours + "h";
    }
    return hours > 0 ? "in " + hours + "h " + minutes % 60 + "m" : "in " + minutes + "m";
  }

  public String date(Instant instant) {
    return instant == null ? unknown : dates.format(instant);
  }

  public String plain(BigDecimal value) {
    return value == null ? unknown : value.stripTrailingZeros().toPlainString();
  }
}
