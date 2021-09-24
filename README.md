# Ritmin

The contracts developed by NekoHitDev.

## Cat Token

This is the token accepted by other contracts (currently only the WCA Contract.)

### Basic info

Supported standard: `NEP-17`

Deployed: 

+ Test net: [`TODO`](https://neo3.testnet.neotube.io/contract/TODO)

+ Main net: [`TODO`](https://neo3.neotube.io/contract/TODO)

### Events and Methods

#### Events

+ `Transfer(from: Hash160, to: Hash160, amount: Integer)`

  Fired when a transaction is successfully processed by contract.

#### Methods

+ `symbol(): String`

  Always return `CAT`, this is the symbol of this token.

+ `decimals(): Integer`

  Always return `2`, this token has decimals of 2.

+ `totalSupply(): Integer`

  Always return `1_000_000_000_00`, this token has a fixed total supply of 1 billion. Discussion can be found here [#5](https://github.com/NekoHitDev/Ritmin/issues/5).

+ `transfer(from: Hash160, to: Hash160, amount: Integer, data: Any): Boolean`

  Transferring some amount of token from `from` account to `to` account. Signature from `from` account is required. Return `true` if and only if this transaction is done.

+ `balanceOf(account: Hash160): Integer`

  Query and return the Cat Token balance of given account.

## WCA Contract

This is the token accepted by other contracts (currently only the WCA Contract.)

### Basic info

Deployed: 

+ Test net: [`TODO`](https://neo3.testnet.neotube.io/contract/TODO)

+ Main net: [`TODO`](https://neo3.neotube.io/contract/TODO)

Note: 

+ Currently, only Cat Token can make a transfer to this contract. Aka only Cat Token can invoke `onNEP17Payment` method. Otherwise, there will be exception.
+ Due to limitations from Neo node implementation, **the identifier of WCA must not longer than 62 bytes**. (Max key size is 64bytes, and all map prefix takes 2 ascii-chars/bytes)

### Events and Methods

See the code, repo wiki, and the white paper.

### How to use

You can use our client to do that, see [NekoHitDev/ritmin-frontend](https://github.com/NekoHitDev/ritmin-frontend).

Or you can manually invoke the methods, see [wiki](https://github.com/NekoHitDev/Ritmin/wiki) for the details.

### Group

Telegram group (Chinese): https://t.me/NekoHitCommunity

Discord Server (English): https://discord.gg/DfSjhXuWyT

Feel free to join the group and ask questions, or submit your ideas. We're glad to hear from you.

## Donate

If you like this project, and want to help the development of this project, please
considering donate some GAS or NEO to this address (Neo N3):

NYukb9Nj59pQZ7SzubZeJUodrhczkXKD1Y

Deploy and update contract use a lot of GAS. Your donation will make our deployment easier.

