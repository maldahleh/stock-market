package com.maldahleh.stockmarket.market;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyBar(
    LocalDate date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
    long volume) {}
