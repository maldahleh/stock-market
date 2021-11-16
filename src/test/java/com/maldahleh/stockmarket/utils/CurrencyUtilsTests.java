package com.maldahleh.stockmarket.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maldahleh.stockmarket.config.Settings;
import java.math.BigDecimal;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CurrencyUtilsTests {

  private Settings settings;

  @BeforeEach
  void setup() {
    this.settings = mock(Settings.class);

    when(settings.getUnknownData())
        .thenReturn("N/A");

    when(settings.getLocale())
        .thenReturn(Locale.US);
  }

  @Nested
  class FormatSingle {

    @Test
    void nullValue() {
      // WHEN
      String formatted = CurrencyUtils.formatSingle(null, settings);

      // THEN
      assertEquals("N/A", formatted);
    }

    @Test
    void validValue() {
      // GIVEN
      BigDecimal value = BigDecimal.valueOf(12.57);

      // WHEN
      String formatted = CurrencyUtils.formatSingle(value, settings);

      // THEN
      assertEquals("12.6", formatted);
    }
  }


  @Nested
  class FormatSigFIg {

    @Test
    void nullValue() {
      // WHEN
      String formatted = CurrencyUtils.formatSigFig(null, settings.getUnknownData());

      // THEN
      assertEquals("N/A", formatted);
    }

    @Test
    void validValue() {
      // GIVEN
      long value = 1_000_000L;

      // WHEN
      String formatted = CurrencyUtils.formatSigFig(value, settings.getUnknownData());

      // THEN
      assertEquals("1.0m", formatted);
    }

    @Test
    void validValueBigDecimal() {
      // GIVEN
      BigDecimal value = BigDecimal.valueOf(1_000_000);

      // WHEN
      String formatted = CurrencyUtils.sigFigNumber(value);

      // THEN
      assertEquals("1.0m", formatted);
    }

    @Test
    void validValueBelowOneThousand() {
      // GIVEN
      long value = 879L;

      // WHEN
      String formatted = CurrencyUtils.formatSigFig(value, settings.getUnknownData());

      // THEN
      assertEquals("879", formatted);
    }
  }
}
