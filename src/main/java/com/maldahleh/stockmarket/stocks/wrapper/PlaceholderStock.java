package com.maldahleh.stockmarket.stocks.wrapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import yahoofinance.Stock;

@Getter
@Setter
@AllArgsConstructor
public class PlaceholderStock {

  private Stock stock;
  private String serverPrice;
}
