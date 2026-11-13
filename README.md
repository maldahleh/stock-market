# StockMarket

![CI](https://github.com/maldahleh/stock-market/workflows/CI/badge.svg)

StockMarket lets players buy and sell real US stocks and crypto with in-game money, at real
market prices. Players can make or lose money on the companies they pick, which gives them a
risk-free way to learn how the stock market works.

## What makes it safe for an economy

- **Real corporate actions.** Splits, reverse splits, stock dividends, renames, cash and stock
  mergers, spin-offs and delistings are applied to everyone's holdings automatically. Fractions
  are paid out in cash, the way a broker does. Without this, a 10-for-1 split would wipe out 90%
  of a holder's value, and a reverse split would print money.
- **No trading on old prices.** Stocks trade only while the US market is open, and every trade
  fetches a fresh price. A price older than `max-price-age-minutes` is refused. This stops players
  from buying on news they've already seen. When the market is closed, players are told when
  it reopens. Selling is always allowed, even below the minimum price, so holders of a falling
  stock can get out.
- **Real dividends.** Cash dividends are paid (times the price multiplier) on the real
  payable date to players who held shares before the ex-dividend date, the same rule a real
  broker uses. Buying after the ex-date doesn't qualify.
- **No duplicated money or shares.** Purchases take the money first and refund it if saving
  fails. Sales remove the shares in the database before paying, so a double click can't sell the
  same shares twice. Every change is written to a permanent ledger.

## Orders

Players can leave standing orders that fill on their own, even while they're offline:

- **Limit buy:** `/stocks order buy AAPL 10 4500` buys 10 shares once the server price is
  4,500 or less.
- **Limit sell:** `/stocks order sell AAPL 10 5200` sells once it's 5,200 or more.
- **Stop-loss:** `/stocks order stop AAPL 10 4000` sells at the market if it falls to 4,000.

Prices are server prices, as shown in the menus. Orders are checked every minute, and stock
orders only fill while the market is open. A triggered order runs at a freshly fetched price, and
a limit order still won't fill outside its limit. Money isn't held while a buy order waits; if
the player can't afford it when it triggers, it's cancelled. Orders expire after 30 days, and a
split, merger or rename cancels open orders on that stock, so a pre-split target can't fill at a
post-split price. `/stocks orders` lists open orders and `/stocks order cancel <id>` cancels one.

## Confirmations

Trades worth `trading.confirm-above` or more (10,000 by default), and selling every share of a
stock, ask for a confirmation first. From a menu, a confirm screen opens. From a command, a
clickable chat message appears (or type `/stocks confirm` within 30 seconds).

## Requirements

- Paper or Spigot 1.21+, Java 21
- **Vault** plus an economy plugin (e.g. EssentialsX)
- A free **Alpaca** account for prices
- **PlaceholderAPI** (optional)

## Setup

1. Sign up at [alpaca.markets](https://alpaca.markets) and open the **paper trading** dashboard.
   No money or brokerage account is needed.
2. Generate an API key there.
3. Put the key ID and secret in `plugins/StockMarket/config.yml` under `alpaca`, then restart.
   The console prints "Connected to Alpaca" when the key works.

The free plan gives real-time prices from the IEX exchange, crypto, and corporate actions,
which is plenty for one server.

**About data terms:** market data providers, including Alpaca, restrict sharing their data with
other people on free plans. Each server uses its own key, so the terms are between that server's
owner and Alpaca. Read them before you run this on a public server.

## Configuration highlights

| Setting | Default | Meaning |
|---|---|---|
| `price-multiplier` | 1 | Server price = market price × this |
| `fees.flat` / `fees.percent` | 10.50 / 0.5 | Charged per trade (percent of trade value) |
| `trading.max-price-age-minutes` | 15 | Refuse trades on prices older than this |
| `trading.minimum-price` | 5.0 | Block penny stocks (USD) |
| `trading.confirm-above` | 10000 | Trades worth this much need a confirmation (0 = off) |
| `trading.bulk-amount` | 5 | Shares bought with shift-click in menus |
| `trading.crypto-click-amount` | 0.01 | Coins one menu click buys or sells |
| `trading.allow-crypto` | true | Allow pairs like `BTC/USD`, 24/7 |
| `dividends` | true | Pay real cash dividends |
| `orders.max-open-per-player` | 10 | Open limit and stop orders per player |
| `orders.expire-days` | 30 | Orders are cancelled after this long |
| `trading.blocked-exchanges` | OTC | Exchanges that can't be traded |
| `storage.type` | sqlite | `sqlite` or `mysql` |

## Commands

Aliases: `/sm`, `/stock`, `/stocks`.

| Command | Permission |
|---|---|
| `/stockmarket help` | `stockmarket.use` |
| `/stockmarket list` | `stockmarket.list` |
| `/stockmarket tutorial` | `stockmarket.tutorial` |
| `/stockmarket lookup <symbol>` | `stockmarket.lookup` |
| `/stockmarket compare <a,b,c>` | `stockmarket.compare` |
| `/stockmarket portfolio [player]` | `stockmarket.portfolio` (`.other` for others) |
| `/stockmarket transactions [player]` | `stockmarket.transactions` (`.other` for others) |
| `/stockmarket history [symbol]` | `stockmarket.history` |
| `/stockmarket buy <symbol> [shares]` | `stockmarket.buy` |
| `/stockmarket sell <symbol> [shares\|all]` | `stockmarket.sell` |
| `/stockmarket confirm` | `stockmarket.use` |
| `/stockmarket order <buy\|sell\|stop> <symbol> <shares> <price>` | `stockmarket.orders` |
| `/stockmarket order cancel <id>` | `stockmarket.orders` |
| `/stockmarket orders` | `stockmarket.orders` |
| `/stockmarket spawnbroker` | `stockmarket.spawnbroker` |
| `/stockmarket removebroker` | `stockmarket.spawnbroker` |
| `/stockmarket orders [player]` | `stockmarket.orders` (`.other` for others) |
| `/stockmarket status` | `stockmarket.reload` |
| `/stockmarket reload` | `stockmarket.reload` (re-reads every file and reconnects) |

`stockmarket.use` (default: everyone) grants every player command. A broker is a villager that
opens the list when right-clicked; it doesn't move or take damage, and `removebroker` deletes
the one nearest you (within 5 blocks). With `broker.disable-commands: true`, players must use one
unless they have `stockmarket.commandbypass`.

Clicking a stock in the list opens its lookup: today's numbers, the past year, your position,
and the last 9 trading days as green (up) and red (down) blocks. Menus have Back, Refresh and
Close, and refresh by themselves when a trade fills. On the lookup's diamond, left click buys 1
(or `crypto-click-amount` of a coin), shift-left buys `bulk-amount` times that, right click
sells the same, and shift-right sells everything you own after a confirmation. The list's bottom
row opens your portfolio, orders, trades, the server's trades and the tutorial, so players can
get around without commands.

Crypto can be bought in fractions: `/stocks buy BTC/USD 0.015`. Crypto has no minimum price.

Anything that happens while a player is offline (orders filling or being cancelled, dividends,
splits) is shown under "While you were away" when they next join.

## For owners

- `/stocks status` shows whether market data is connected, how many players hold stocks, open
  orders, the last warning, and money bought, sold, paid in fees and paid in dividends over the
  last 7 and 30 days. It works from the console.
- `/stocks reload` re-reads `config.yml` and every menu and message file, then reconnects, so a
  key typo doesn't need a restart. Admins see a reminder on join while trading is off.
- From the console, `/stocks portfolio <player>`, `/stocks transactions <player>` and
  `/stocks orders <player>` print text. Moderators with `stockmarket.orders.other` can view and
  cancel anyone's orders.
- `trading.broadcast-above` announces big trades to everyone (off by default).
- The market opening and closing is announced in chat, and players see a one-line portfolio
  summary when they join. Blank the `market-news` or `portfolio-on-join` messages to turn them
  off.

## Placeholders

- `%sm_portfolio-value%`: the player's holdings at current server prices
- `%sm_top-<n>-name%`, `%sm_top-<n>-gain%`: the leaderboard, ranked by percent gain, refreshed
  every 5 minutes. Players with less than `leaderboard.minimum-invested` aren't ranked.
- `%sm_rank%`: the player's place on the leaderboard, or `-`
- `%sm_sd-<symbol>-<point>%`: a stock's data, where point is `name`, `sp` (server price),
  `price` (market price in USD), `vol` or `change` (percent since yesterday's close).
  Example: `%sm_sd-AAPL-sp%`. Shows N/A until the first price loads.

## Developers

`StockPurchaseEvent` and `StockSaleEvent` fire after a trade is saved and paid.

## Upgrading from 3.x

Version 4 is a rewrite. It uses new database tables, and 3.x trades are not carried over. The
menu files changed too: delete or rename the old `.yml` files in `plugins/StockMarket` so the new
ones are created.

The default stock list was updated: `FB` now belongs to an unrelated ETF, so Facebook is listed
as `META`, and the delisted `K`, `TWX` and `TWTR` were replaced with Tesla, JPMorgan and AMD.
