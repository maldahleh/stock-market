package com.maldahleh.stockmarket.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CurrencyUtils {

  public String format(BigDecimal input, String unknown, Locale locale) {
    if (input == null) {
      return unknown;
    }

    return formatCurrency(input.doubleValue(), locale);
  }

  public String formatSigFig(Long input, String unknown) {
    if (input == null) {
      return unknown;
    }

    return sigFigNumber(input);
  }

  public String formatSingle(BigDecimal input, String unknown, Locale locale) {
    if (input == null) {
      return unknown;
    }

    return singleDecimal(input.doubleValue(), locale);
  }

  public String sigFigNumber(double input) {
    String suffixes = "kmbt";
    if (input < 1000) {
      return String.valueOf(input).replace(".0", "");
    }

    int exponent = (int) (Math.log(input) / Math.log(1000));
    return String.format("%.1f%c", input / Math.pow(1000, exponent), suffixes.charAt(exponent - 1));
  }

  public String formatCurrency(double input, Locale locale) {
    NumberFormat decimalFormat =
        new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(locale));
    return decimalFormat.format(input);
  }

  private String singleDecimal(double input, Locale locale) {
    NumberFormat decimalFormat = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(locale));
    return decimalFormat.format(input);
  }
}
