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

### How to get some

Currently we have a telegram bot [dofovn_bot](https://t.me/dufovn_bot) to do the airdrop.

+ Use `/airdrop` command, then send bot your address, like `NPLa4dfW8sNLj9j71Qg1JcFjUp36waDEBr`, then you will receive 50 cat token on Neo N3 RC3 test net. **You can request this everyday** (count in UTC, you can query the server side time by using `/time`).
+ Use `/balance`, then send bot your address, you can query the CAT token balance of the given address. 

+ If something goes wrong, you can check the status of this bot by using `/status` command. This will reply you a status report and a report email address, feel free to report the malfunction status to this email box.

## WCA Contract

This is the token accepted by other contracts (currently only the WCA Contract.)

### Basic info

Deployed: 

+ RC3 Test net: `0x11ed46dd463f850b628b27e632532157fb6200bd`

+ Main net: `TODO`

Permission: `*`

Trust: `*`

Note: 

+ Currently only Cat Token can make a transfer to this contract. Aka only Cat Token can invoke `onNEP17Payment` method. Otherwise there will be exception.
+ Due to limitations from Neo node implementation, **the identifier of WCA must not longer than 54 bytes**. (Max key size is 64bytes, and all map prefix takes 10 ascii-chars/bytes)

### Events and Methods

#### Events

+ `CreateWCA(creator: Hash160, identifier: String, milestoneCount: Integer)`

  Fired when successfully created a WCA.

+ `PayWCA(owner: Hash160, identifier: String, amount: Integer)`

  Fired when owner successfully paid the stake.

+ `BuyWCA(buyer: Hash160, identifier: String, amount: Integer)`

  Fired when buyer successfully made a purchase. The amount refers to this transaction, not total purchased amount.

+ `FinishMilestone(identifier: String, milestoneIndex: Integer, proofOfWork: String)`

  Fired when owner successfully finished a milestone.

+ `FinishWCA(identifier: String)`

  Fired when a WCA is  successfully finished (by the meaning of all tokens are transferred properly).

+ `Refund(buyer: Hash160, identifier: String, returnToBuyerAmount: Integer, returnToCreatorAmount: Integer)`

  Fired when buyer successfully made a refund.

  

#### Methods

+ `createWCA(owner: Hash160, stakePer100Token: Integer, maxTokenSoldCount: Integer, descriptions: String[], endTimestamps: int[], thresholdIndex: Integer, coolDownInterval: Integer, identifier: String): String`

  Create a WCA. Signature from owner account is required.

  + `stakePer100Token`: Stake rate per 1.00 CAT, represent in fraction: 100 means 1.00 CAT.

  + `maxTokenSoldCount`: How much token would you sell to buyer, represent in fraction.

  + `descriptions`: Array of string, describing each milestones.

  + `endTimestamps`: Array of timestamp (millisecond), represent the deadline of each milestones.

  + `thresholdIndex`: The index of your threshold milestone. Before finish this specific milestone, buyer can make a full refund, after finish this milestone, buyer can only make refund for the token corresponding to milestones you haven't finished yet, the token corresponding to milestones you finished will transfer to owner when buyer make a refund.

  + `coolDownInterval`: Limit your rate of finishing milestones. You must wait this interval before you can finish next milestone. This will prevent evil creator just finishes all milestones and take tokens away. By this limitation, buyer can have time notice the abnormalities and have time to make a refund. 

  + `identifier`: Just give your WCA a identifier, this must unique to other WCAs.

    

+ `onNEP17Payment(from: Hash160, amount: Integer, identifier: String): Unit`

  Handle transaction from Cat Token. If transaction made from the owner of this WCA, then this will be handled as stake; otherwise it will be handled as general purchase.

  Stake amount = `stakePer100Token` * `maxTokenSoldCount` / 100. **You must pay the stake in one shot.** 

  For example, `stakePer100Token = 10` and  `maxTokenSoldCount = 1000_00`, means I want to sell 1000.00CAT in total and stake 0.10CAT per token I sold, thus I need stake 1000.00 * 0.10 = 100.00CAT.

  For general purchase, you can make multiple purchase for one WCA, your purchases will be accumulated and treated like single one.

  

+ `finishMilestone(identifier: String, index: Integer, proofOfWork: String): Unit`

  Finish a milestone in your WCA. Owner's signature is required.

  You cannot finish a missed milestone. Which means you cannot finish milestone#5, then finish milestone#4, this is not allowed. Also you cannot finish a expired (miss the deadline) milestone.

  The `proofOfWork` not necessarily need to be your work. It can be a link to some URL, or Cid from IPFS, or just some explanation of how buyer can find and evaluate your work. 

  Please do notice that you cannot modify `proofOfWork` after you finished a milestone. So do think twice before call this method.

  

+ `finishWCA(identifier: String): Unit`

  Finish a WCA. By finish, it means account all tokens and transfer to each buyer and owner based on milestones. Only owner can finish WCA if there has any unfinished milestone left. If the last milestone is expired, any one can request to finish this WCA. If the last milestone is finished by `finishMilestone`, this method will be called automatically.

  For all buyer stick to the end:

  + Your total amount = your purchased amount + your purchased mount * stakePer100Token

    For example: I purchased 100.00CAT and stake rate is 0.10CAT, then total amount is 110.00CAT

  + Return to buyer amount = your total amount * unfinished milestone count / total milestone count.

    For example there are 3 milestones in total, owner only finished 2, so I will get my 110.00CAT * 1 / 3 = 36.66CAT back, including the stake as a punishment for owner not finishing that milestone.

  + After sending those token back to each buyer, all left token are transferred to owner.

    

+ `refund(identifier: String, buyer: Hash160): Unit`

  Make a refund. Signature from buyer account is required.

  If you make a refund before owner finishing the threshold milestone, then you can get all your token back. Your purchase record will be deleted.

  If you make a refund after owner finishing the threshold milestone:

  + Send to creator amount = your purchased amount * finished milestone count / total milestone count

  + Rest of the token will send back to your account.

    For example, I purchased 100.00CAT and currently owner finished 2 milestone, total 5. Then I will lose 100.00 * 2 / 5 = 40.00CAT to owner, and get rest 60.00CAT back.

    

+ `queryWCA(identifier: String): Json`

  Query a WCA by given identifier. If identifier not exists, then empty string will be returned, otherwise the corresponding NeoVM version of Json will be returned:

  ```Java
  public class WCAPojo {
      // Base64 encoded little-endian owner Hash160
      public String ownerBase64;
      // stake per 1.00 token
      public int stakePer100Token;
      // total tokens can sell, in fraction.
      // i.e.: 100 means 1.00CAT
      public int maxTokenSoldCount;
      // if stake is paid by owner
      public boolean stakePaid;
      // total milestone count
      public int milestonesCount;
      // milestone details
      public List<WCAMilestone> milestones;
      // the threshold milestone
      public int thresholdMilestoneIndex;
      // the cool down interval, in ms
      public int coolDownInterval;
      // last time this WCA is updated, in ms
      public int lastUpdateTimestamp;
      // next to be done milestone index
      public int nextMilestone;
      // tokens remained for sell
      public int remainTokenCount;
      // total buyer count
      public int buyerCount;
  }
  
  public class WCAMilestone {
      // description of this milestone
      public String description;
      // the deadline of this milestone, in ms
      public int endTimestamp;
      // how to find the work result
      // null means this milestone is not finished yet
      public String linkToResult;
  }
  ```

  

+ `queryPurchase(identifier: String, buyer: Hash160): Integer`

  Query the total purchase amount of a given WCA and buyer address. This is required since the underlaying structure is a block of data, so **only one modification (one purchase) is accepted per block, rest of them are discarded.** You can use this method to check if your purchase is accepted.

### How to use

You can use our client to do that, see [NekoHitDev/ritmin-frontend](https://github.com/NekoHitDev/ritmin-frontend).

Or you can manually invoke the methods. Here is the procedure：

1.  Make sure you have enough GAS to operate. Typical gas usage will be listed for each step.

2. Create WCA by invoking the `createWCA` method, giving the correct parameters.

   + Create a 2 milestones WCA will cost 0.4470761GAS
   + Create a 10 milestones WCA will cost 0.6622909GAS

   + Create a 50 milestones WCA will cost 1.7737549GAS
   + Create a 250 milestones WCA will cost 7.4834049GAS
   + The gas usage also depends on how much bytes you write into storage.
   + 2 milestones with each has 60 bytes description will cost 0.5642561GAS
   + 10 milestones with each has 60 bytes description will cost 1.2481109GAS
   + 50 milestones with each has 60 bytes description will cost 4.7027549GAS
   + 250 milestones with each has 60 bytes description will cost 22.1284049GAS

3. Pay the stake, by transfer the correct amount Cat Token to WCA Contract. Address can be found in `Basic Info` section. This usually costs less than 0.5GAS.
4. Let buyer make the purchase, by transfer the correct amount Cat Token to WCA Contract. Address can be found in `Basic Info` section. This will rewrite all purchase record again, but with 75% discount. Also you have to pay for instructions that handle this transaction, and the fee to write your purchase data into storage. 
   + Each record consist of a Hash160 and a integer, Let's assume they take 32 bytes, plus two integer, take that as 32 too. If we have 1000 buyers, then old data is 3232 bytes, this will charge you 0.808GAS, then your new data is 32 bytes, cost 0.032GAS, so 0.84GAS in total.
5. Finish milestones, by calling the `finishMilestone` method. This will rewrite all milestone data again, but with 75% discount. Also you have to pay for instructions that handle this transaction, and the fee to write your `proofOfWork` into storage. If your are finishing the last milestone, then you have to cover the fee for finish the WCA, aka the step 6.
6. Finish the WCA. Most of fees are used to pay for instructions that transfer tokens. This is based on how many buyers, each transaction cost less than 0.15GAS.



### TODO

Currently milestone data are stored in basic info, which means all changes to basic info (paid flag, finished flag, etc.) required rewrite all milestones data, though there is a 75% discount for rewrite, but still a big chunk of data, especially when you relay on `description` to explain how your milestone is defined.

For now we recommend you have a separate place to store those large chunk of description.

In the future, we plan to split those milestone data in a separate map, so changes to basic info won't rewrite them, this will benefit operations like paying the stake, finish the WCA.

Also we are trying to make out a plan to reduce purchase fee when there are already a large amount purchase record. But it cannot be simply spited as basic info and milestone data.
