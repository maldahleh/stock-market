package com.maldahleh.stockmarket.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuantitiesTest {

  @Test
  void stocksAreWholeShares() {
    assertEquals(5, Quantities.parse("AAPL", "5"));
    assertThrows(NumberFormatException.class, () -> Quantities.parse("AAPL", "1.5"));
    assertThrows(NumberFormatException.class, () -> Quantities.parse("AAPL", "0"));
    assertEquals(0, new BigDecimal("500").compareTo(Quantities.value("AAPL",
        new BigDecimal("100"), 5)));
  }

  @Test
  void cryptoIsCountedInHundredMillionths() {
    assertEquals(1_500_000, Quantities.parse("BTC/USD", "0.015"));
    assertEquals("0.015", Quantities.amount("BTC/USD", 1_500_000).toPlainString());
    assertEquals(1, Quantities.parse("BTC/USD", "0.00000001"));
    assertThrows(NumberFormatException.class, () -> Quantities.parse("BTC/USD", "0.000000001"));
    assertEquals(0, new BigDecimal("900").compareTo(Quantities.value("BTC/USD",
        new BigDecimal("60000"), 1_500_000)));
  }
}
