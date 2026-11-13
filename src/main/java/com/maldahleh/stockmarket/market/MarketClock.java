package com.maldahleh.stockmarket.market;

import java.time.Instant;

/** Regular-session status of the US stock market. */
public record MarketClock(boolean open, Instant nextOpen, Instant nextClose, Instant fetchedAt) {}
