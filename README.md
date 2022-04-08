# Ritmin

The contracts developed by NekoHitDev.

## Cat Token

This is our native token, which has a fixed rate with fUSDT: 1 CAT = 0.5 fUDST.

### Basic info

Supported standard: `NEP-17`

Symbol: `CAT`

Decimals: `2`

Total supply: `Not fixed`

Deployed:

+ Test
  net: [`0xf461dff74f454e5016421341f115a2e789eadbd7`](https://neo3.testnet.neotube.io/contract/0xf461dff74f454e5016421341f115a2e789eadbd7)
+ Test net address: `NiC9K2Z2kSuCTmhempiw5TcPHuee6PTsDE`
+ Main
  net: [`0xcdc17669ce3b7cfa65a29c4941aba14dbff9b12b`](https://neo3.neotube.io/contract/0xcdc17669ce3b7cfa65a29c4941aba14dbff9b12b)
+ Main net address: `NPu1SxtZf2PDDGwzbdWQd2h6Gi5kXiyKM5`

### Mint from fUSDT

You can mint CAT by transferring some amount of fUSDT to cat contract. The cat contract will mint your cat tokens in the
same tx. Note: The tx will be rejected if you send unchangeable amount, like 0.000001 fUSDT.

### Destroy and get fUSDT

You have to invoke the `destroyToken` method with your script hash and cat amount as parameter, along with your
signature (`CallByEntity` is enough, don't use `Global`). The cat contract will destroy and send the fUSDT to your
wallet in the same tx.

## WCA Contract

This is the core contract that handle the crowdfunding protocol.

### Basic info

Deployed:

+ Test net (public
  test): [`0x514e4dc6398ba12a8c3a5ed96187d606998c4d93`](https://neo3.testnet.neotube.io/contract/0x514e4dc6398ba12a8c3a5ed96187d606998c4d93)

+ Test net (develop
  use): [`0x3d151c524c35ea5cd549323d98e782cfb7403951`](https://neo3.testnet.neotube.io/contract/0x3d151c524c35ea5cd549323d98e782cfb7403951)

+ Main
  net: [`0x1312460889ef976db3561e7688b077f09d5e98e0`](https://neo3.neotube.io/contract/0x1312460889ef976db3561e7688b077f09d5e98e0)

Note:

+ Due to limitations from Neo node implementation, **the identifier of WCA must not longer than 62 bytes**. (Max key
  size is 64bytes, and all map prefix takes 2 ascii-chars/bytes)

### Events and Methods

See the code, repo wiki, and the white paper.

### How to use

You can use our client to do that, see [NekoHitDev/ritmin-frontend](https://github.com/NekoHitDev/ritmin-frontend).

Or you can manually invoke the methods, see [wiki](https://github.com/NekoHitDev/Ritmin/wiki) for the details.

## Nekoin Token

This is our DAO token.

### Basic info

Supported standard: `NEP-17`

Symbol: `NEKOIN`

Decimals: `8`

Total supply: `Not fixed`

Deployed:

+ Test
  net: [`0x0ee8873f41537b7e14cbeb8d9d72176e7d137aee`](https://neo3.testnet.neotube.io/contract/0x0ee8873f41537b7e14cbeb8d9d72176e7d137aee)
+ Test net address: `NhevFQjT1bPGKcpc9gFCCrXkce1rEWJozt`
+ Main net: [`TODO`](https://neo3.neotube.io/contract/TODO)
+ Main net address: `TODO`

### Mint&Destroy

Mint&Destroy can only be done by the contract owner.

### Read&Write message

The contract has `writeMessage(ByteString content): ByteString` and `readMessage(ByteString index): ByteString` methods.
The `writeMessage` method will hash (SHA-256) the content and use that as the index. It will save the content and return
the index, meanwhile fire a `WriteMessage<Index>` event. If the content has already stored, an exception will be thrown.
Assuming the SHA-256 will not collide, the index will represent the same content, no matter who wrote it. So
the `writeMessage` won't validate any signature.

For reading the content out, use `readMessage`, input the index and the content will be returned. The returned value is
non-null since it will throw an exception if the index is not found.

## Group

Telegram group (Chinese): https://t.me/NekoHitCommunity

Discord Server (English): https://discord.gg/DfSjhXuWyT

Feel free to join the group and ask questions, or submit your ideas. We're glad to hear from you.

## Donate

If you like this project, and want to help the development of this project, please considering donate some GAS or NEO to
this address (Neo N3):

NWWpkYtqeUwgHfbFMZurmKei6T85JtA1HQ

Deploy and update contract use a lot of GAS. Your donation will make our deployment easier.

