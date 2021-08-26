# Ritmin

The contracts developed by NekoHitDev.

## Cat Token

This is the token accepted by other contracts (currently only the WCA Contract.)

### Basic info

Supported standard: `NEP-17`

Deployed: 

+ RC4 Test net: [`0xf461dff74f454e5016421341f115a2e789eadbd7`](https://neo3.testnet.neotube.io/contract/0xf461dff74f454e5016421341f115a2e789eadbd7)

+ Main net: `TODO`

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

+ RC4 Test net: [`0xcb74c96a1a65ff3b07662294d2da243543587403`](https://neo3.testnet.neotube.io/contract/0xcb74c96a1a65ff3b07662294d2da243543587403)

+ Main net: `TODO`

Note: 

+ Currently only Cat Token can make a transfer to this contract. Aka only Cat Token can invoke `onNEP17Payment` method. Otherwise there will be exception.
+ Due to limitations from Neo node implementation, **the identifier of WCA must not longer than 54 bytes**. (Max key size is 64bytes, and all map prefix takes 10 ascii-chars/bytes)

### Events and Methods

See the code, repo wiki, and the whitepaper.

### How to use

You can use our client to do that, see [NekoHitDev/ritmin-frontend](https://github.com/NekoHitDev/ritmin-frontend).

Or you can manually invoke the methods. 

### Group

Telegram group (Chinese): https://t.me/NekoHitCommunity

Discord Server (English): https://discord.gg/DfSjhXuWyT



Feel free to join the group and ask questions, or submit your ideas. We're glad to hear from you.
