# Ritmin

The contracts developed by NekoHitDev.

## Cat Token

This is the token accepted by other contracts (currently only the WCA Contract.)

### Basic info

Supported standard: `NEP-17`

Deployed: 

+ RC3 Test net: [`0xf461dff74f454e5016421341f115a2e789eadbd7`](https://neo3.neotube.io/contract/0xf461dff74f454e5016421341f115a2e789eadbd7)

+ Main net: `TODO`

Permission: `*`

Trust: `*`

### Events and Methods

#### Events

+ `Transfer(from: Hash160, to: Hash160, amount: Integer): Unit`

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

### How to get some

Currently we have a telegram bot [dofovn_bot](https://t.me/dufovn_bot) to do the airdrop.

+ Use `/airdrop` command, then send bot your address, like `NPLa4dfW8sNLj9j71Qg1JcFjUp36waDEBr`, then you will receive 50 cat token on Neo N3 RC3 test net. **You can request this everyday** (count in UTC, you can query the server side time by using `/time`).
+ Use `/balance`, then send bot your address, you can query the CAT token balance of the given address. 

+ If something goes wrong, you can check the status of this bot by using `/status` command. This will reply you a status report and a report email address, feel free to report the malfunction status to this email box.

## WCA Contract

TODO

