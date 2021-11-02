package com.maldahleh.stockmarket.placeholder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import yahoofinance.Stock;

@Data
@AllArgsConstructor
public class PlaceholderStock {

  private Stock stock;
  private String serverPrice;
}
