package com.maldahleh.stockmarket.commands.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.maldahleh.stockmarket.commands.util.CommandUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommandUtilsTests {

  @Test
  void buildOtherPermission() {
    // GIVEN
    String basePerm = "base";

    // WHEN
    String otherPerm = CommandUtils.buildOtherPermission(basePerm);

    // THEN
    assertEquals("base.other", otherPerm);
  }

  @Nested
  class DetermineQuantity {
    @Test
    void lessThanMinArgs() {
      // GIVEN
      String[] args = new String[0];

      // WHEN
      int quantity = CommandUtils.determineQuantity(args);

      // THEN
      assertEquals(1, quantity);
    }

    @ParameterizedTest
    @ValueSource(strings = {"test", "-5", "9.6"})
    void invalidQuantity(String invalidValue) {
      // GIVEN
      String[] args = new String[]{"test", "test", invalidValue};

      // WHEN
      int quantity = CommandUtils.determineQuantity(args);

      // THEN
      assertEquals(-1, quantity);
    }

    @Test
    void validQuantity() {
      // GIVEN
      String[] args = new String[]{"test", "test", "4"};

      // WHEN
      int quantity = CommandUtils.determineQuantity(args);

      // THEN
      assertEquals(4, quantity);
    }
  }
}
