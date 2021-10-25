package com.maldahleh.stockmarket.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class StockPurchaseEventTests {

  @Test
  void testProperties() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    StockPurchaseEvent stockPurchaseEvent = new StockPurchaseEvent(
        player,
        "BA",
        5,
        BigDecimal.ONE,
        BigDecimal.ZERO,
        BigDecimal.ONE
    );

    // THEN
    assertEquals(player, stockPurchaseEvent.getPlayer());
    assertEquals("BA", stockPurchaseEvent.getStockSymbol());
    assertEquals(5, stockPurchaseEvent.getQuantity());
    assertEquals(BigDecimal.ONE, stockPurchaseEvent.getStockValue());
    assertEquals(BigDecimal.ZERO, stockPurchaseEvent.getBrokerFees());
    assertEquals(BigDecimal.ONE, stockPurchaseEvent.getGrandTotal());

    assertNotNull(stockPurchaseEvent.getHandlers());
    assertNotNull(StockPurchaseEvent.getHandlerList());
  }
}
