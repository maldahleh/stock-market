package com.maldahleh.stockmarket.placeholder.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class PlaceholderStockTests {

  @Test
  void equals() {
    EqualsVerifier.simple()
        .forClass(PlaceholderStock.class)
        .verify();
  }
}
