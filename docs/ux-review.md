# StockMarket v4 UX review

Branch `real-market`, reviewed from the code and YAML only. Nothing has been run in game. Paths are relative to `src/main/`.

**Verification.** The six reviewers sent 60 findings, 10 per lens. They merge into 33 items below, and 4 of those were added in this review (U12, U15, U23, plus the part of U2 about trade estimates and orders that keep retrying).

- **Dropped 1:** "Don't build watchlists" (engagement). It proposes no change.
- **Kept with corrections:**
  - The "try BTC when the market is closed" advice is wrong. Crypto trades in whole units, so at ×20 one BTC costs about 2,000,000 server money (see U12).
  - The leaderboard idea's reasoning is wrong. Ranking by absolute gain still favours rich players, so it is changed to percent.
  - Two engagement findings cite OrderBook line numbers that don't exist (984-1017; the file has 346 lines). The behaviour they describe is real.
  - "The order check makes 60 requests an hour" is only true while orders are open, because `check()` returns early when there are none. When it runs, it also calls the clock each time.
  - "Middle-click does nothing in survival" can't be confirmed from code and needs a check in game.

---

## Fix before release

**U1. A left-click on a list item buys a share**
- **Today:** In `Menus.tradeClick`, LEFT calls `trader.buy(...,1,true)` and SHIFT_LEFT buys `bulk-amount`. A confirm only appears at 10,000 or more (`config.yml trading.confirm-above`). Every `list.yml` item has 13 lore lines, and 10 of them explain five click types. The lookup is opened with Q or middle-click (`MIDDLE, DROP`). A new player who clicks to inspect a stock buys it and pays 10.50 + 0.5% in fees. After the Tiingo switch they won't find out for up to 90 seconds.
- **Change:** Any click on a list item opens the lookup. Trading stays on the lookup's diamond, which already shows the price and fee. Cut each list lore to 3 lines: price and today's change, you own, "Click for details". This removes the buy/sell branches for list items and about 300 lines of `list.yml`. If the owner wants one-click trading kept, the fallback is shortening the controls to the lookup's 2 lines and always confirming a player's first trade.
- **Effort:** S

**U2. Design the Tiingo switch around a single price refresher**
- **Today:** Each part of the plugin requests prices on its own:
  - Orders are checked every `orders.check-seconds` (60). Each check makes a quote call plus a clock call (`StockMarketPlugin.schedule`, `OrderBook.check`).
  - Placeholders refresh each symbol every 30 s, one request per symbol. `%sm_portfolio-value%` does this for every holding (`StockExpansion.quote`).
  - `openList` fetches all 30 symbols each time `cache-seconds` runs out.
  - Every trade calls `freshQuote` with `Duration.ZERO`. For a buy below the confirm limit, `Trader.request` first makes a separate `market.quotes` call just to estimate the cost (added in this review).
  - A lookup fetches the asset, a quote and 252 bars, and bars are cached for only 1 hour (`MarketData.BARS_TTL`).
  - Triggered orders that fail for a reason that isn't fatal retry and refetch on every check (added in this review). For example, a limit buy below `minimum-price` retries on every check until it expires, because PRICE_TOO_LOW and STALE_PRICE aren't in `OrderBook.FATAL`.
  - At 50 requests an hour, the default setup would run out before anyone opened a menu.
- **Change:**
  - Run one batched refresh about every 72 s. It covers held symbols, `list.yml` symbols, open-order symbols and queued trades.
  - Orders, queued trades, menus, placeholders and confirm estimates read only from that cache. Placeholders never start a fetch.
  - `portfolio-value` uses the last known price instead of turning the whole line into N/A.
  - Keep bars until the next close (or 12 h) and assets for 24 h. Lookups of new symbols use whatever budget is left.
  - Delete `cache-seconds`, `orders.check-seconds` and the `StockExpansion.REFRESH` timer instead of tuning them.
- **Effort:** M (part of the switch)

**U3. Messages for the 0-90 second fill**
- **Today:** `Trader.execute` sends `pending.trade` ("Getting the latest price...") and nothing else. BUSY says "Your last trade is still processing." `confirm.details` says "The final price is set when the trade runs." The menu stays open showing the old "You own" (rendered once in `openList`). `TradeService.inFlight` allows one trade per player, so a second click within 90 s gets BUSY. A 2 s cooldown applies on top of that.
- **Change:**
  - Replace `pending.trade` for stocks with "Queued: buy &6<quantity> <symbol>&e at the next price update (usually under 90s). We'll tell you when it fills." Keep the current line for crypto, which fills instantly.
  - When a trade is queued from a menu, close the menu.
  - Start the bought/sold blocks with "Filled:" and play a Spigot sound when the trade fills.
  - Reword BUSY to "You already have a trade waiting for the next price update" and `confirm.details` to "Fills at the next price update, usually within 90 seconds."
  - See open question 5 for the cooldown and the one-trade lock.
- **Effort:** M

**U4. Fills, cancellations and corporate actions that happen offline are never reported**
- **Today:** `actionNotifier`, `orderNotifier.filled` and `orderNotifier.cancelled` all return early when `Bukkit.getPlayer` is null. `Trader.report` drops results for offline players. Cancelled orders are deleted (`OrderBook.remove` → `deleteOrder`) and never appear in the ledger. The README promises orders "fill on their own, even while they're offline". So a player whose stop-loss expired or was cancelled by a split finds it gone with no reason given.
- **Change:** Add one `sm_notices` table (owner, rendered line, time). Each notifier writes to it when the player is offline. One `PlayerJoinEvent` listener sends "&6While you were away:" plus up to about 10 lines, then deletes them. This also covers queued trades that settle after the player logs out. It uses none of the request budget.
- **Effort:** M

**U5. "Couldn't load" is reported as "doesn't exist", and the list shows N/A with no explanation**
- **Today:** In `Menus.load()`, an error from the asset fetch returns `Optional.empty()`, so `openLookup` sends `failures.INVALID_SYMBOL` ("AAPL isn't a listed symbol"). Compare drops failed symbols silently, or sends "No data was found." `openList` replaces a failed fetch with `Map.of()` and opens 30 items priced N/A that can still be clicked to trade. `pending.menu` is defined but never sent. Under the hourly budget this will happen regularly.
- **Change:** Make `load()` tell "not found" apart from "couldn't load", and send `failures.DATA_UNAVAILABLE` for the second. If the list's quotes come back empty, send the same line once, or open the list from `cachedQuote` with an "as of HH:mm" line. In compare, name each symbol that was left out. Send `pending.menu` or delete it.
- **Effort:** S

**U6. The large-trade confirm is skipped when no estimate is available, and it never shows fees**
- **Today:** In `Trader.request`, a quote error becomes `Map.of()`, so the estimate is null, `large` is false, and `execute()` runs. `/stocks buy NVDA 10000` therefore goes straight through whenever the estimate fetch fails. When the confirm does show, it says "About <estimate> before fees".
- **Change:** If `confirmAbove > 0` and there's no estimate, ask anyway (estimate shown as N/A) or refuse with DATA_UNAVAILABLE. Never execute silently. Add `<fees>` and `<total>` from `rules.fee(estimate, buying)`, worded "pay about" for buys and "receive about" for sells. After U2, take the estimate from the cache.
- **Effort:** S

**U7. Broker-only mode leaves players stranded in the list**
- **Today:** With `broker.disable-commands: true`, `StockCommand.runAsPlayer` refuses everything except the broker commands. `Brokers.onInteract` only opens the list. Players can buy but can't see their portfolio, trades, orders or the tutorial. The two identical "Thousands more" books (`list.yml` 32 and 35) tell them to type commands they can't use.
- **Change:** Either remove the option and `stockmarket.commandbypass` (simplest), or add an optional `action:` key to `ItemTemplate` (portfolio, transactions or tutorial) and put those items in the list's empty slots 33/34. Either way, delete the duplicate book in slot 35. See open question 2.
- **Effort:** S

**U8. The tutorial is wrong, out of date, hard to read, and doesn't explain this plugin**
- **Today:**
  - `tutorial.yml` cites "Sony (Symbol: SNE)", which is now SONY.
  - It defines volume as "the number of shares issued". That is wrong, and it contradicts the lookup's Volume item.
  - It includes a Yahoo URL that can't be clicked.
  - Its last book points to "/stock help", and help points back to the tutorial.
  - Every lore line is `&8`, which is barely readable.
  - `openTutorial` passes `Map.of()`, so no config values can appear.
  - It never covers the ×multiplier, fees, market hours, the delayed fill, orders, or splits and dividends.
- **Change:** Five short books (6 lines or fewer, `&7` text with `&f` highlights):
  1. What a share is.
  2. Prices here: real price × `<multiplier>`, plus the fees on both buying and selling.
  3. When you can trade: US hours and crypto at any time. Stock trades fill at the next price update.
  4. How to trade: click a stock, then use the diamond. One order example.
  5. Splits and dividends arrive by themselves, with a chat notice.

  Pass the rule values into `openTutorial`.
- **Effort:** S

**U9. Config, messages and README still describe Alpaca and its settings**
- **Today:**
  - `config.yml` has the `alpaca.*` block, `feed`, `live-account`, `blocked-exchanges` ("Alpaca exchange codes"), and `cache-seconds` ("Trades always fetch a fresh price").
  - `messages.yml not-configured` says "add an Alpaca key".
  - The README says "Connected to Alpaca", but the code logs "Connected to market data." The README also says "every trade fetches a fresh price" and "Orders are checked every minute".
  - The list book's `BTC/USD` example depends on the Coinbase symbol format.
- **Change:** Replace the block with `tiingo: key: ""`, a three-step comment and a terms note. Add one line: "Free keys allow 50 requests an hour: stock trades fill at the next refresh, usually within 90 seconds." Delete the settings that no longer do anything (see U2). Keep `blocked-exchanges` only if Tiingo or the SEC list supplies an exchange. Log a one-line hint if an old `alpaca.key-id` is still present. Keep `BTC/USD` as the player-facing format and map it internally. Make the README match the real log text.
- **Effort:** S

**U10. Placing an order never shows the current price, and some orders fill at once or never**
- **Today:** `OrderBook.place()` checks the asset but never a quote. `placed.*` shows only `<target>`. A player thinking in dollars sets a stop at 220 on a stock priced at 4,600, and it never triggers. A stop above the market (`triggeredBy`: price ≤ target) sells at the next check. A limit buy above the market fills at once. A limit buy on a symbol below `minimum-price` is accepted and then retries silently until it expires (added in this review).
- **Change:**
  - Read `cachedQuote` in `place()` (no request) and add "(now <current>)" to the `placed.*` messages.
  - Add "This will fill at the next check" when the target has already been reached.
  - Add "Order prices are server prices, not US dollars" when the target is far below the price, by a factor of the multiplier.
  - Reject buy orders the trade itself would refuse (below `minimum-price`).
- **Effort:** S

**U11. A rejected key looks like "try again shortly" and fills the console**
- **Today:** `checkConnection` logs one line, but `start()` has already enabled trading. A 401 during a trade becomes DATA_UNAVAILABLE ("Try again shortly"). `OrderBook.check` logs a full stack trace on every check. Only `ActionService.warnedUnavailable` warns just once.
- **Change:** Remember an auth-failed state on 401/403. While it is set, players get "&cThe market isn't connected. Tell an admin.", order checks are skipped, and the console gets one line an hour naming the key setting. Reuse the warn-once pattern, and drop the stack trace for `MarketDataException`.
- **Effort:** S

**U12. Crypto is mostly out of reach (added in this review)**
- **Today:** Shares are `long` everywhere, so crypto trades in whole coins. At ×20, one BTC at $100k costs 2,000,000 server money, and one ETH costs tens of thousands. `TradeService.quoteProblem` applies `minimum-price` ($5) to crypto too, so most cheap coins can't be bought. That leaves crypto mostly unusable, even though it's the only thing that trades on evenings and weekends, when most players are online. Separately, `sale()` refuses a sale whose value doesn't cover the fees (PRICE_TOO_LOW), which contradicts the README's "Selling is always allowed".
- **Change:** Apply `minimum-price` to stocks only. If crypto gets list slots, choose coins that are affordable at the default multiplier. See open question 4 on fractional crypto. Give the sale case its own message ("Fees are more than 1 share is worth; sell more at once").
- **Effort:** S (without fractional amounts)

**U13. Corporate-action notices are hard-coded, use jargon, and mix up the money**
- **Today:** `ActionService.describe()` builds English text in Java ("split 4-for-1", "removed as worthless", "paid a dividend of $0.51 a share"). The `$0.51` is the real USD rate, but the payout is in server money (×20). The `corporate-action` template says "Your holdings were updated" even for a dividend, when nothing changed. None of it can be translated or recoloured. The rename text will also be used for renames and delistings from the SEC list.
- **Change:** Add one `messages.yml` key per action type, with plain wording, for example "each share is now 4; your total value didn't change", or "stopped trading and was removed". Show the dividend per share in server money, or leave it out. Keep `describe()` for console logs only.
- **Effort:** M

## Quick wins

**U14. Show what one share really costs, and put numbers in the "not enough" messages**
- **Today:** `list.yml` shows "Price: <server-price>" with no fee and no multiplier. The lookup shows "Server Price" next to "Market Price $187.50 USD" with no explanation, and the currency is printed twice (`Formats.usd` adds `$`, and the lore appends `<market-currency>`). INSUFFICIENT_FUNDS and INSUFFICIENT_SHARES get only `<symbol>` (`Trader.report`). Sell-all with 0 shares says "You don't own that many shares". PRICE_TOO_LOW doesn't say the limit is in real dollars, or that selling is still allowed.
- **Change:** Add `<buy-one-total>` and `<multiplier>` to `StockView`. Use "1 share: X with fees" and "real price × <multiplier>". Drop `<market-currency>` after USD values. Send "That costs <cost> with fees; you have <balance>", "You own <owned> shares of <symbol>" and "You don't own any <symbol>". Reword PRICE_TOO_LOW with `<minimum>` and "you can still sell it".
- **Effort:** S

**U15. The portfolio's gain or loss ignores the selling fee**
- **Today:** `holdingValues` and `portfolioTotals` calculate value minus cost basis. The sold message nets out the sell fee (`settleSale`). So the portfolio can show +5.00 and the sale then report -6.60.
- **Change:** Show "If sold now (after fees)": value minus `rules.fee(value,false)`. Use it in totals and `%sm_portfolio-value%` too. Optionally add a break-even price.
- **Effort:** S

**U16. "Market closed" gives fixed hours, doesn't say when it reopens, and is checked after the fetch**
- **Today:** MARKET_CLOSED hard-codes "9:30 to 4:00 New York time on weekdays", with no AM/PM, no holidays and no time zone help. `TradeService.price()` fetches the asset, the clock and a fresh quote together, then refuses, so the budget is spent anyway. Nothing in the menus shows the market is closed. Orders can be placed while it's closed, but the message doesn't say so.
- **Change:**
  - Check the clock first for stocks.
  - Send "Opens in <opens-in>. Orders can be placed now (/stocks order)."
  - Add a `<market-status>` placeholder to the list and lookup lore.
  - Tiingo has no market clock, so after the switch the open and close times must come from a local NYSE calendar with holidays and early closes (added in this review). That gives `nextOpen` without spending requests.
- **Effort:** S

**U17. Misused commands say only "Invalid syntax"**
- **Today:** Misused lookup, compare, buy and sell all send `invalid-syntax`, even though `messages.yml commands.*` already has a usage line for each.
- **Change:** Send "Try: " plus `commands.<sub>` for a known subcommand. Keep "Unknown command. See /stocks help." for anything else.
- **Effort:** S

**U18. Limit messages don't say what the limit is**
- **Today:** TOO_MANY_SHARES, COOLDOWN and TOO_MANY_ORDERS state no numbers. `OrderBook.inputProblem` maps shares above the maximum to INVALID_QUANTITY ("not a valid number").
- **Change:** Pass `<max>`, `<seconds>` and `<max-orders>`, and return TOO_MANY_SHARES from `OrderBook`.
- **Effort:** S

**U19. Order wording, unused keys and duplicate keys**
- **Today:**
  - The `commands.order` help line is about 80 characters, wraps, and never explains "stop".
  - The CORPORATE_ACTION cancel reason wraps to 3 lines.
  - "Order #4 filled." doesn't say whether it was a buy or a sell.
  - `failures.PRICE_MOVED` is never shown, because order failures that aren't fatal retry silently.
  - `invalid-quantity` and most `orders.problems.*` copy `failures.*`, and the two DATA_UNAVAILABLE lines are worded differently.
  - INVALID_SYMBOL and NOT_TRADABLE start with an extra `&e`.
- **Change:** Split the help into a limit line and a stop line. Shorten the cancel reason. End `orders.filled` with ":" so the receipt follows. Delete PRICE_MOVED and the duplicates, falling back from `orders.problems.X` to `failures.X`.
- **Effort:** S

**U20. Managing orders means typing IDs, and selling by hand can quietly break a stop-loss**
- **Today:** Cancelling needs `/stocks order cancel <id>`, read from a plain chat line. Tab completion stops at the order type, and `buy` has no completion at all. `OrderBook` checks shares only when an order is placed, so selling by hand leaves stop and sell orders that are later cancelled for "not enough shares".
- **Change:**
  - Add a clickable `[Cancel]` to each `/stocks orders` line, using the same ClickEvent as `askInChat`.
  - Tab-complete held symbols and open order IDs.
  - When a sale leaves too few shares for an open sell or stop order, add "Your stop-loss #<id> now needs more shares than you own" to the sold message.
- **Effort:** S

**U21. Gains and losses are all yellow**
- **Today:** `Formats.percent` and `signedMoney` return plain text, and every use in the YAML is wrapped in `&e`.
- **Change:** Colour signed values in `StockView` and `holdingValues`: `&a` positive, `&c` negative, `&7` zero or unknown. Leave `Formats` plain so PlaceholderAPI output stays plain. Remove the `&e` in front of them in the YAML.
- **Effort:** S

**U22. Empty paged menus look like errors**
- **Today:** The empty state is a BARRIER named "No data was found." in slot 0. "No previous page" and "no next page" are red barriers. There is no page count. The transactions and history items can be clicked but don't say so.
- **Change:** Use a glass pane, or leave the slot empty, when there's no page. Add `<page>/<pages>`. Add an optional `empty:` item to `PagedTemplate` ("No shares yet. Buy from /stocks list or a broker"). Add "Click to look up" to the ledger lore.
- **Effort:** S

**U23. Broker clicks: two-hand events, missing permission, and the plugin not set up (added in this review)**
- **Today:** `Brokers.onInteract` doesn't check `event.getHand()`, and Spigot fires `PlayerInteractEntityEvent` once per hand, so the list may open twice (check in game). A player without `stockmarket.list` gets nothing at all. Brokers are registered only in `start()`, so with no key or a failed start they do nothing.
- **Change:** Ignore the off-hand event. Send `no-permission`. Register the listener in `onEnable` and send `not-configured` when services are null.
- **Effort:** S

**U24. Warn owners that changing the multiplier revalues everything**
- **Today:** Cost basis and order targets are stored in server money. `serverPrice` uses the current multiplier. Moving it from 20 to 10 halves every holding and can trigger stop-losses on the next check. The `config.yml` comment doesn't warn about this.
- **Change:** Add a comment: "Set this before players trade. Changing it later revalues every holding and can trigger open orders." Optionally log one example line at startup ("At ×20, 1 AAPL ≈ 4,600"). Skip the stored-multiplier machinery.
- **Effort:** S

## Worth building

**U25. Redesign the lookup**
- **Today:** `lookup.yml` has 15 identical BOOKs and two identical DIAMONDs (slots 3 and 5). "Currency" is always USD and "Symbol" repeats the title. Server-money and USD values sit side by side. The 12 daily bars (slots 18-26, then the leftover 30-32) run newest-first from the left (`Menus` reverses them) as identical enchanted books. Missing data shows 12 barriers.
- **Change:** A 27-slot menu: name, you own, one trade diamond, "Today (real market, USD)", "Past year (real market, USD)". Below that, 9 bars from oldest to newest in slots 9-17, using LIME or RED concrete through `up:` and `down:` templates, and one pane when there is no history. Mostly a YAML change.
- **Effort:** S-M

**U26. Back and refresh in menus, and menus that update after a trade**
- **Today:** A lookup opened from the portfolio, compare or ledger has no way back. Confirm and Cancel both call `closeInventory()`, and Cancel gives no message. After a fill, the open menu still shows the old "You own". Ctrl+Q (CONTROL_DROP) isn't handled. The "Middle Click" hint may not work in survival (check in game).
- **Change:** Add an optional `Runnable back` to `openLookup` and the confirm screen, with Back, Refresh (reads the cache) and Close items. Confirm and Cancel return to the menu the player came from. In `Trader.report`, reopen the player's open `MenuHolder`. Change the hint to "Q (drop)" only, and map CONTROL_DROP as well.
- **Effort:** M

**U27. Label compare rows**
- **Today:** `compare.yml` uses 18 generic books per stock, giving 6 unlabelled rows for 3 stocks.
- **Change:** Use one row of 9 per stock, reusing U25's grouped items, with a NAME_TAG of the symbol first. Three stocks fit in 27 slots, lined up in columns.
- **Effort:** S

**U28. `/stocks status` for owners**
- **Today:** There is no health view. The only signals are a startup line and scattered warnings.
- **Change:** One console-friendly command, under the reload permission. It shows: key OK, rejected or missing; market open or closed; requests used out of 50 and when the budget resets; next refresh; queued trades; open orders; the last error and its time. It also shows one 7/30-day `SUM(cash) GROUP BY type` line covering money spent, received, fees and dividends. Log one line the first time the budget runs out in an hour.
- **Effort:** M

**U29. `/stocks reload` re-reads config.yml and starts the services**
- **Today:** With no key, `onEnable` returns early. `reloadFiles()` reloads only messages and menus, and its message says "Restart the server". A key typo costs a restart.
- **Change:** Reload rebuilds the services (shutting down the old executors and registering listeners once) and replies "Connected to Tiingo; market is open" or the exact error. Ops get a one-line notice on join while the plugin isn't set up.
- **Effort:** M

## Ideas to consider later

- **U30. One portfolio line on join, plus today's change.** "Your portfolio: <value> (<today> today, <net> overall)", built from cached quotes only. Add `<today-value>` and `<today-percent>` to `portfolioTotals` and the portfolio Stats item. Blanking the message turns it off. S.
- **U31. Market open and close announcements.** Two message keys (blank turns them off), from the local calendar in U16, checked on an existing timer. No requests. S.
- **U32. Leaderboard placeholders.** Rank by percent gain, with a minimum cost basis, every 5 minutes from the U2 cache. `%sm_top-<n>-name%`, `%sm_top-<n>-gain%`, `%sm_rank%`. This needs U2 first. M.
- **U33. Optional big-trade broadcast and small moderator tools.** Announce buys at or above `confirm-above` using the existing events, with the message blank by default. For moderators: `/stocks orders <player>` with cancel, console-readable portfolio and transactions, and player names next to the UUID in the "pay it by hand" logs. S each. Skip `/stocks adjust` unless support needs it.

## Open questions for the owner

1. **List clicks:** should list items only open the lookup (removes code), or keep one-click trading with shorter lore (U1)?
2. **`broker.disable-commands`:** remove it, or keep it and add navigation items to the list (U7)?
3. **Offline news:** add a small notices table (U4), or just a join line pointing to `/stocks transactions`? The join line misses cancelled orders, because they never reach the ledger.
4. **Crypto:** allow fractional crypto, or accept that BTC and ETH are out of reach at ×20 and only exempt crypto from `minimum-price` (U12)?
5. **Queued fills:** keep one trade per player, or one per symbol settled in the same batch? And can `trading.cooldown-seconds` go once fills are batched (U3)?
6. **Default multiplier:** at 20, most list stocks cost thousands per share. Does that suit typical server balances, or should the default be lower?
7. **Closed market:** should a stock trade placed while the market is closed keep being refused, or queue for the open like an order?

## Owner decisions (2026-11-13)

1. **List clicks:** any click on a list item opens the lookup; trading only from the lookup (U1).
2. **Broker-only mode:** keep `broker.disable-commands` and add navigation items (portfolio, orders, history) to the list menu (U7).
3. **Offline news:** notices table shown on join (U4).
4. **Crypto:** investigate fractional amounts before deciding (U12).
5. **Queued fills:** one waiting trade per player; drop `trading.cooldown-seconds` (U3).
6. **Default multiplier:** lower from 20 to 1.
7. **Closed market:** keep refusing stock trades, but say when the market reopens (U16).

## Built (2026-11-13)

- From the owner decisions: fractional crypto, list clicks open the lookup, list navigation row,
  offline notices, confirm when no estimate plus fees shown (U6), reopen time (part of U16), no
  cooldown, default multiplier 1.
- Second round: U14, U15, U16 (market status in menus), U17, U18, U19, U20, U21, U22, U23, U24,
  U25, U26, U27, U28 (without the request budget, which comes with Tiingo), U29, U30, U31, U32,
  U33.
- Still open: U1 is done; U2, U3, U9 and U11 come with the Tiingo switch; U5, U8, U10, U12
  (crypto list slots) and U13 remain.
