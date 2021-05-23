package info.skyblond.nekohit.neo.contract;

import io.neow3j.devpack.Runtime;
import info.skyblond.nekohit.neo.pojo.WCABasicInfo;
import info.skyblond.nekohit.neo.pojo.WCABuyerInfo;
import info.skyblond.nekohit.neo.pojo.WCAPojo;
import io.neow3j.devpack.*;
import io.neow3j.devpack.annotations.*;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event4Args;
import io.neow3j.devpack.events.Event5Args;

@ManifestExtra(key = "name", value = "WCA and CAT Token Contract")
@ManifestExtra(key = "github", value = "https://github.com/NekoHitDev/GwenDarcy")
@SupportedStandards("NEP-17")
public class CatToken {
    // for private net test
    private static final Hash160 owner = StringLiteralHelper.addressToScriptHash("NVCqzVkjApBWtgKa7c7gbURrJ4dmFYLekS");
    // for public test net
//    private static final Hash160 owner = StringLiteralHelper.addressToScriptHash("NgcUHzmpxH6aFgaDq8bL93P7X7uUi5kbBD");

    private static final StorageContext storageContext = Storage.getStorageContext();

    @OnVerification
    public static boolean verify() throws Exception {
        checkPrivilege();
        return true;
    }

    @OnDeployment
    public static void deploy(Object data, boolean update) throws Exception {
        checkPrivilege();
        if (!update) {
            if (totalSupply() > 0) {
                throw new Exception("Contract was already deployed.");
            }
            // Initialize token supply
            Storage.put(storageContext, tokenTotalSupplyKey, initialSupply);
            // And allocate all tokens to the contract owner.
            tokenAssetMap.put(owner.asByteString(), initialSupply);
        }
    }


    // CAT token with NEP-17 standard
    @DisplayName("Transfer")
    private static Event3Args<Hash160, Hash160, Integer> onTransfer;
    private static final String tokenAssetPrefix = "CAT";
    private static final int initialSupply = 10_000_000_00;
    private static final String tokenTotalSupplyKey = "tokenTotalSupply";
    private static final StorageMap tokenAssetMap = storageContext.createMap(tokenAssetPrefix);

    // NEP-17 functions
    public static int totalSupply() {
        return Storage.get(storageContext, tokenTotalSupplyKey).toInteger();
    }

    public static String symbol() {
        return "CAT";
    }

    public static int decimals() {
        return 2;
    }

    public static int balanceOf(Hash160 account) throws Exception {
        if (!account.isValid()) {
            throw new Exception("Argument is not a valid address.");
        }
        return getTokenBalance(account);
    }

    public static boolean transfer(Hash160 from, Hash160 to, int amount, Object[] data) throws Exception {
        if (!from.isValid() || !to.isValid()) {
            throw new Exception("From or To address is not a valid address.");
        }
        if (amount < 0) {
            throw new Exception("The transfer amount was negative.");
        }
        if (!Runtime.checkWitness(from) && from != Runtime.getCallingScriptHash()) {
            throw new Exception("Invalid sender signature. The sender of the tokens needs to be "
                    + "the signing account.");
        }
        if (getTokenBalance(from) < amount) {
            return false;
        }
        if (from != to && amount != 0) {
            deductTokenFromBalance(from, amount);
            addTokenToBalance(to, amount);
        }

        onTransfer.fire(from, to, amount);
        if (ContractManagement.getContract(to) != null) {
            Contract.call(to, "onNEP17Payment", CallFlags.ALL, data);
        }

        return true;
    }

    // token helper functions
    private static void addTokenToBalance(Hash160 key, int value) {
        tokenAssetMap.put(key.asByteString(), getTokenBalance(key) + value);
    }

    private static void deductTokenFromBalance(Hash160 key, int value) {
        int oldValue = getTokenBalance(key);
        if (oldValue == value) {
            tokenAssetMap.delete(key.asByteString());
        } else {
            tokenAssetMap.put(key.asByteString(), oldValue - value);
        }
    }

    private static int getTokenBalance(Hash160 key) {
        return tokenAssetMap.get(key.asByteString()).toInteger();
    }

    // WCA things, each addr has their own wca info map
    @DisplayName("CreateWCA")
    private static Event5Args<Hash160, Integer, Integer, Integer, String> onCreateWCA;
    @DisplayName("BuyWCA")
    private static Event4Args<Hash160, Hash160, String, Integer> onBuyWCA;
    @DisplayName("FinishWCA")
    private static Event3Args<Hash160, String, Boolean> onFinishWCA;

    private static final StorageMap wacBasicInfoMap = storageContext.createMap("WCA_BASIC_INFO");
    private static final StorageMap wacBuyerInfoMap = storageContext.createMap("WCA_BUYER_INFO");

    private static ByteString getTrueId(Hash160 owner, String identifier) {
        return owner.asByteString().concat(identifier);
    }

    private static WCABasicInfo getWCABasicInfo(ByteString trueId) {
        ByteString data = wacBasicInfoMap.get(trueId);
        if (data == null) {
            return null;
        }
        return (WCABasicInfo) StdLib.deserialize(data);
    }

    private static WCABuyerInfo getWCABuyerInfo(ByteString trueId) {
        ByteString data = wacBuyerInfoMap.get(trueId);
        if (data == null) {
            return null;
        }
        return (WCABuyerInfo) StdLib.deserialize(data);
    }

    public static String queryWCA(Hash160 owner, String identifier) {
        ByteString trueId = getTrueId(owner, identifier);
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            return "";
        }

        WCABuyerInfo buyerInfo = getWCABuyerInfo(trueId);
        if (buyerInfo == null) {
            return "";
        }

        WCAPojo result = new WCAPojo(
                basicInfo.stakePer100Token,
                basicInfo.maxTokenSoldCount,
                buyerInfo.remainTokenCount,
                basicInfo.endTimestamp
        );

        return StdLib.jsonSerialize(result);
    }

    public static void createWCA(Hash160 owner, int stakePer100Token, int maxTokenSoldCount, int endTimestamp, String identifier) throws Exception {
        if (!owner.isValid()) {
            throw new Exception("Owner address is not a valid address.");
        }
        if (stakePer100Token <= 0) {
            throw new Exception("The stake amount per 100 token was non-positive.");
        }
        if (maxTokenSoldCount <= 0) {
            throw new Exception("The max sell token count was non-positive.");
        }
        if (endTimestamp <= Runtime.getTime()) {
            throw new Exception("The end timestamp was in the past.");
        }
        if (!Runtime.checkWitness(owner) && owner != Runtime.getCallingScriptHash()) {
            throw new Exception("Invalid sender signature. The sender of the tokens needs to be "
                    + "the signing account.");
        }

        int totalStake = stakePer100Token * maxTokenSoldCount / 100;
        if (getTokenBalance(owner) < totalStake) {
            throw new Exception("Insufficient account balance.");
        }

        ByteString trueId = getTrueId(owner, identifier);

        // identifier should be unique
        if (wacBasicInfoMap.get(trueId) != null) {
            throw new Exception("Duplicate identifier.");
        }
        // create wca info obj
        WCABasicInfo info = new WCABasicInfo(stakePer100Token, maxTokenSoldCount, endTimestamp);
        ByteString basicData = StdLib.serialize(info);
        ByteString buyerData = StdLib.serialize(new WCABuyerInfo(maxTokenSoldCount));
        // remove token from creator
        deductTokenFromBalance(owner, totalStake);
        // store
        wacBasicInfoMap.put(trueId, basicData);
        wacBuyerInfoMap.put(trueId, buyerData);

        onCreateWCA.fire(owner, stakePer100Token, maxTokenSoldCount, endTimestamp, identifier);
    }

    public static int buyWCA(Hash160 buyer, Hash160 owner, String identifier, int amount) throws Exception {
        if (!buyer.isValid() || !owner.isValid()) {
            throw new Exception("Buyer or Owner address is not a valid address.");
        }
        if (amount <= 0) {
            throw new Exception("The token amount was non-positive.");
        }
        if (!Runtime.checkWitness(buyer) && buyer != Runtime.getCallingScriptHash()) {
            throw new Exception("Invalid sender signature. The sender of the tokens needs to be "
                    + "the signing account.");
        }
        if (getTokenBalance(buyer) < amount) {
            throw new Exception("Insufficient account balance.");
        }

        ByteString trueId = getTrueId(owner, identifier);

        // identifier should be unique
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            throw new Exception("Identifier not found.");
        }

        if (basicInfo.endTimestamp <= Runtime.getTime()) {
            throw new Exception("You can't buy a terminated WCA.");
        }

        WCABuyerInfo buyerInfo = getWCABuyerInfo(trueId);
        if (buyerInfo == null) {
            throw new Exception("Buyer info not found.");
        }
        if (buyerInfo.remainTokenCount < amount) {
            throw new Exception("Insufficient token remain in this WCA.");
        }

        // update buyer info
        buyerInfo.addBuyer(buyer, amount);
        // remove token from buyer
        deductTokenFromBalance(buyer, amount);

        // store
        ByteString data = StdLib.serialize(buyerInfo);
        wacBuyerInfoMap.put(trueId, data);
        onBuyWCA.fire(buyer, owner, identifier, amount);
        return amount;
//        return wacBuyerInfoMap.get(trueId).length();
//        return data.length() + trueId.length();
    }

    public static boolean finishWCA(Hash160 owner, String identifier, boolean finished) throws Exception {
        if (!owner.isValid()) {
            throw new Exception("Owner address is not a valid address.");
        }
        if (finished) {
            // only creator can update WCA to finished
            if (!Runtime.checkWitness(owner) && owner != Runtime.getCallingScriptHash()) {
                throw new Exception("Invalid caller signature. The caller needs to be "
                        + "the owner account.");
            }
            // otherwise anyone can request to check if wca is end
        }

        ByteString trueId = getTrueId(owner, identifier);

        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            throw new Exception("Identifier not found.");
        }
        // get wca buyer info obj
        WCABuyerInfo buyerInfo = getWCABuyerInfo(trueId);
        if (buyerInfo == null) {
            throw new Exception("Buyer info not found.");
        }

        if (finished) {
            // Creator cannot finish a terminated WCA
            if (basicInfo.endTimestamp <= Runtime.getTime()) {
                throw new Exception("You can't finish a terminated WCA.");
            }
            // all amount goes to creator, return stake to creator
            addTokenToBalance(owner, buyerInfo.totalAmount + basicInfo.stakePer100Token * basicInfo.maxTokenSoldCount / 100);
            // remove wca
            wacBasicInfoMap.delete(trueId);
            wacBuyerInfoMap.delete(trueId);
            onFinishWCA.fire(owner, identifier, true);
        } else if (basicInfo.endTimestamp <= Runtime.getTime()) {
            // otherwise, if wca is end, return amount+stake to buyer
            // return remain stake to creator
            for (int i = 0; i < buyerInfo.buyer.size(); i++) {
                int stake = buyerInfo.amount.get(i) * basicInfo.stakePer100Token / 100;
                addTokenToBalance(buyerInfo.buyer.get(i), buyerInfo.amount.get(i) + stake);
            }

            // return remaining stake
            if (buyerInfo.remainTokenCount > 0) {
                addTokenToBalance(owner, buyerInfo.remainTokenCount * basicInfo.stakePer100Token / 100);
            }
            // remove wca
            wacBasicInfoMap.delete(trueId);
            wacBuyerInfoMap.delete(trueId);
            onFinishWCA.fire(owner, identifier, false);
        } else {
            // nothing is done.
            return false;
        }

        return true;
    }

    // admin tools, owner only
    private static void checkPrivilege() throws Exception {
        if (!Runtime.checkWitness(owner)) {
            throw new Exception("No authorization.");
        }
    }

    public static Hash160 contractOwner() {
        return owner;
    }

    // for token management
    public static boolean issueNewToken(int amount) throws Exception {
        checkPrivilege();
        if (amount <= 0) {
            return false;
        }
        addTokenToBalance(owner, amount);
        int currentAmount = Storage.get(storageContext, tokenTotalSupplyKey).toInteger();
        Storage.put(storageContext, tokenTotalSupplyKey, currentAmount + amount);
        return true;
    }

    public static boolean destroyToken(int amount) throws Exception {
        checkPrivilege();
        if (amount <= 0) {
            return false;
        }
        if (getTokenBalance(owner) < amount) {
            return false;
        }
        deductTokenFromBalance(owner, amount);
        int currentAmount = Storage.get(storageContext, tokenTotalSupplyKey).toInteger();
        Storage.put(storageContext, tokenTotalSupplyKey, currentAmount - amount);
        return true;
    }

    // for contract management
    public static void update(ByteString nefFile, String manifest) throws Exception {
        checkPrivilege();
        if (nefFile.length() == 0 && manifest.length() == 0) {
            throw new Exception("The new contract script and manifest must not be empty.");
        }
        ContractManagement.update(nefFile, manifest, null);
    }

    public static void destroy() throws Exception {
        checkPrivilege();
        ContractManagement.destroy();
    }

}
