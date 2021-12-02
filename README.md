# StockMarket

![CI](https://github.com/maldahleh/stock-market/workflows/CI/badge.svg)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/3176d5c3c5764d85a3036d78b7518fc8)](https://www.codacy.com/gh/maldahleh/stock-market/dashboard)

**Java 16+ required**

StockMarket is a Minecraft plugin that allows players to buy or sell
real stocks using in-game currency. This provides players a way to
make or lose money based on the performance of the stocks they pick,
providing a risk-free way for players to learn about the stock market.

## Dependencies

**Vault + Economy Plugin** - Used for transactions.

**Citizens** - _Optional_ - Used for brokers.

**PlaceholderAPI** - _Optional_ - Used for placeholders.

## PlaceholderAPI Placeholders

### Player Placeholders

**%sm_portfolio-value%** - Displays the player's portfolio value.

### Stock Placeholders

**%sm_sd-\<symbol>-\<point>%** - Display information on a stock.
You may see N/A while data is loaded in.

#### Available Points

**name** - Example: %sm_sd-ba-name% - Return the full company name.

**volume** - Example: %sm_sd-ba-vol% - Return the stock's volume.

**cap** - Example: %sm_sd-ba-cap% - Return the stock's market cap.

**server price** - Example: %sm_sd-ba-sp% - Return the stock's server price.

In the above examples, BA (Boeing) is used as the stock symbol.

## Commands and Permissions

### Permissions

_stockmarket.use_ - Required to use any command

_stockmarket.commandbypass_ - Bypass broker command restrictions

### Commands

#### Help Commands

**/stockmarket** OR **/stockmarket help** - Displays help message

**/stockmarket list** - _stockmarket.list_ - Displays popular stocks

**/stockmarket tutorial** - _stockmarket.tutorial_ - Displays tutorial

#### Lookup Commands

**/stockmarket lookup {symbol}** - _stockmarket.lookup_ - Lookup a stock

**/stockmarket compare {comma separated list of symbols}** -
_stockmarket.compare_ - Compare a list of stocks

**/stockmarket portfolio** - _stockmarket.portfolio_ -
View your portfolio

**/stockmarket portfolio {player}** - _stockmarket.portfolio.other_ -
View another player's portfolio

**/stockmarket transactions** - _stockmarket.transactions_ - View your
transaction history

**/stockmarket transactions {player}** - _stockmarket.transactions.other_ -
View another player's transaction history

**/stockmarket history** - _stockmarket.history_ -
View the server's 100 most recent transactions

**/stockmarket history {symbol}** - _stockmarket.history_ - View the
transaction history of a stock

#### Transaction Commands

**/stockmarket buy {symbol} {amount (default: 1)}** - Sell stock

**/stockmarket sell {symbol} {amount (default: 1)}** - Buy stock

#### Broker Commands

**/stockmarket simplebroker** - _stockmarket.spawnbroker_ - Spawn a simple
broker (opens list inventory) at the executor's location

### Command Aliases

-	/sm
-	/stock
-	/stocks

## Developers

### API

_getPortfolioValue(UUID uuid)_ - Get the portfolio value of the specified player

_getProfitMargin(UUID uuid)_ - Get the profit margin of the specified player

_getPlayerStocks(UUID uuid)_ - Get the specified player's stocks

### Events

**StockPurchaseEvent** - Called when a purchase transaction occurs

**StockSaleEvent** - Called when a sale transaction occurs
