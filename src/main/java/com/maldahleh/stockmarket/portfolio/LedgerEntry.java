package com.maldahleh.stockmarket.portfolio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One change to a player's shares or money, never edited once written.
 *
 * @param shares signed change in shares of {@code symbol}
 * @param price server price per share, or null when no price applies
 * @param cash signed change in the player's balance (positive means paid to the player)
 * @param reference the corporate action id that caused it, if any
 */
public record LedgerEntry(
    UUID owner,
    EntryType type,
    String symbol,
    long shares,
    BigDecimal price,
    BigDecimal fee,
    BigDecimal cash,
    Instant time,
    String reference) {}
