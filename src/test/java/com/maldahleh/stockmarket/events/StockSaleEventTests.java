package com.maldahleh.stockmarket.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class StockSaleEventTests {

  @Test
  void testProperties() {
    // GIVEN
    Player player = mock(Player.class);

    // WHEN
    StockSaleEvent stockSaleEvent = new StockSaleEvent(
        player,
        "BA",
        5,
        BigDecimal.ONE,
        BigDecimal.ZERO,
        BigDecimal.ONE,
        BigDecimal.TEN,
        BigDecimal.ZERO
    );

    // THEN
    assertEquals(player, stockSaleEvent.getPlayer());
    assertEquals("BA", stockSaleEvent.getStockSymbol());
    assertEquals(5, stockSaleEvent.getQuantity());
    assertEquals(BigDecimal.ONE, stockSaleEvent.getStockValue());
    assertEquals(BigDecimal.ZERO, stockSaleEvent.getBrokerFees());
    assertEquals(BigDecimal.ONE, stockSaleEvent.getGrandTotal());
    assertEquals(BigDecimal.TEN, stockSaleEvent.getInitialPurchase());
    assertEquals(BigDecimal.ZERO, stockSaleEvent.getNetOnTransaction());

    assertNotNull(stockSaleEvent.getHandlers());
    assertNotNull(StockSaleEvent.getHandlerList());
  }
}
