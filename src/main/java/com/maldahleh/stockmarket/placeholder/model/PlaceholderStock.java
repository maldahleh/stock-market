package com.maldahleh.stockmarket.placeholder.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import yahoofinance.Stock;

@Data
@NoArgsConstructor
public class PlaceholderStock {

  private Stock stock;
  private String serverPrice;
}
