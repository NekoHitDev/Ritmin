package info.skyblond.nekohit.neo.contract;

import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;
import static info.skyblond.nekohit.neo.helper.Utils.require;
import static info.skyblond.nekohit.neo.contract.WCAAuxiliary.*;
import info.skyblond.nekohit.neo.domain.*;
import info.skyblond.nekohit.neo.helper.Pair;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Contract;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnNEP17Payment;
import io.neow3j.devpack.annotations.OnVerification;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Trust;
import io.neow3j.devpack.constants.CallFlags;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event4Args;

@ManifestExtra(key = "name", value = "WCA Contract")
@ManifestExtra(key = "github", value = "https://github.com/NekoHitDev/Ritmin")
@ManifestExtra(key = "author", value = "NekoHitDev")
@Trust(value = "*")
@Permission(contract = "*")
public class WCAContract {
    // for public net
    static final Hash160 OWNER = addressToScriptHash("NV5CSGyT6B39fZJ6zw4x6gh1b3C6cpjTm3");

    // address of CatToken Hash.
    // For private test net deploy by genesis(gradle): NiMNN2ZXML7C9uNEnp66U3VNp38FLBcJQi
    // For private test net deploy by genesis(vsc): NjFMoMSoukNBetDZYPsGKzpLrUA1zgkMNM
    // For public net deploy by NV5C...jTm3: NfbKv3Rg6grgkLVG7SJYtPmhJXcW43RzbH
    static final Hash160 CAT_TOKEN_HASH = addressToScriptHash("NiMNN2ZXML7C9uNEnp66U3VNp38FLBcJQi");

    private static final StorageContext CTX = Storage.getStorageContext();
    private static final StorageMap wcaBasicInfoMap = CTX.createMap("BASIC_INFO");
    private static final StorageMap wcaMilestonesMap = CTX.createMap("MILESTONES");
    private static final StorageMap wcaBuyerInfoMap = CTX.createMap("BUYER_INFO");
    private static final StorageMap wcaIdentifierMap = CTX.createMap("IDENTIFIER");

    // creator, identifier, milestone count
    @DisplayName("CreateWCA")
    private static Event3Args<Hash160, String, Integer> onCreateWCA;

    // owner, identifier, amount
    @DisplayName("PayWCA")
    private static Event3Args<Hash160, String, Integer> onPayWCA;

    // buyer, identifier, deal amount
    @DisplayName("BuyWCA")
    private static Event3Args<Hash160, String, Integer> onBuyWCA;

    // identifier, milestone index, proof of work
    @DisplayName("FinishMilestone")
    private static Event3Args<String, Integer, String> onFinishMilestone;

    // identifier
    @DisplayName("FinishWCA")
    private static Event1Arg<String> onFinishWCA;

    // buyer, identifier, return to buyer amount, return to creator amount
    @DisplayName("Refund")
    private static Event4Args<Hash160, String, Integer, Integer> onRefund;


    @OnNEP17Payment
    public static void onPayment(Hash160 from, int amount, Object data) throws Exception {
        require(CAT_TOKEN_HASH == Runtime.getCallingScriptHash(), "Only Cat Token can invoke this function.");
        require(amount > 0, "Transfer amount must be a positive number.");
        var identifier = (String) data;
        WCABasicInfo basicInfo = getWCABasicInfo(identifier);
        require(basicInfo != null, "Identifier not found.");
        List<WCAMilestone> milestones = getWCAMilestones(identifier);
        require(milestones != null, "Identifier not found.");

        if (basicInfo.owner.equals(from)) {
            // owner paying stake
            require(!basicInfo.paid, "You can't pay a paid WCA.");
            require(!checkIfReadyToFinish(milestones), "You can't pay a expired WCA.");
            require(basicInfo.getTotalStake() == amount, "Amount not correct");
            // unpaid before, not finished(expired), amount is correct
            basicInfo.paid = true;
            wcaBasicInfoMap.put(identifier, StdLib.serialize(basicInfo));
            onPayWCA.fire(from, identifier, amount);
        } else {
            // buyer want to buy a WCA
            throwIfNotAvailableToBuy(basicInfo, milestones);
            WCABuyerInfo buyerInfo = getWCABuyerInfo(identifier);
            require(buyerInfo != null, "Buyer info not found.");
            buyerInfo.recordPurchase(from, amount);
            wcaBuyerInfoMap.put(identifier, StdLib.serialize(buyerInfo));
            onBuyWCA.fire(from, identifier, amount);
        }
    }

    public static String queryWCA(String identifier) {
        WCABasicInfo basicInfo = getWCABasicInfo(identifier);
        if (basicInfo == null) {
            return "";
        }

        WCABuyerInfo buyerInfo = getWCABuyerInfo(identifier);
        if (buyerInfo == null) {
            return "";
        }

        List<WCAMilestone> milestones = getWCAMilestones(identifier);
        if (milestones == null) {
            return "";
        }

        WCAPojo result = new WCAPojo(basicInfo, milestones, buyerInfo);

        return StdLib.jsonSerialize(result);
    }

    public static int queryPurchase(String identifier, Hash160 buyer) {
        WCABuyerInfo buyerInfo = getWCABuyerInfo(identifier);
        if (buyerInfo == null) {
            return 0;
        }
        if (!buyerInfo.purchases.containsKey(buyer)) {
            return 0;
        }
        return buyerInfo.purchases.get(buyer);
    }

    /**
     * Request to create a WCA with given params.
     * 
     * @param owner             ScriptHash of WCA owner, need to be the signer of
     *                          this tx
     * @param stakePer100Token  stake price for 1.00 token, represent in fraction
     * @param maxTokenSoldCount total tokens for buyer
     * @param descriptions      descriptions for milestone
     * @param endTimestamps     endTimestamps for each milestone
     * @param thresholdIndex    the index of threshold milestone
     * @param coolDownInterval  cool down time between finish two milestones, miliseconds
     * @param identifier        the name of this WCA
     * 
     * @return the global unique id for this WCA
     */
    public static String createWCA(
        Hash160 owner, int stakePer100Token, int maxTokenSoldCount, 
        String[] descriptions, int[] endTimestamps, int thresholdIndex,
        int coolDownInterval, String identifier
    ) throws Exception {
        require(Runtime.checkWitness(owner) || owner == Runtime.getCallingScriptHash(), "Invalid sender signature. The owner of the wca needs to be the signing account.");
        // identifier should be unique
        require(wcaBasicInfoMap.get(identifier) == null, "Duplicate identifier.");
        // check milestone
        require(descriptions.length == endTimestamps.length, "Cannot decide milestones count.");

        // convert to object on the fly
        List<WCAMilestone> milestones = new List<>();
        int lastTimestamp = 0;
        for (int i = 0; i < endTimestamps.length; i++) {
            require(lastTimestamp < endTimestamps[i], "The end timestamp should increase.");
            require(endTimestamps[i] > Runtime.getTime(), "The end timestamp is already expired.");
            lastTimestamp = endTimestamps[i];
            milestones.add(new WCAMilestone(descriptions[i], endTimestamps[i]));
        }

        // create wca info obj
        WCABasicInfo info = new WCABasicInfo(
            owner, stakePer100Token, maxTokenSoldCount, 
            milestones.size(), thresholdIndex, coolDownInterval
        );

        ByteString basicData = StdLib.serialize(info);
        ByteString milestoneData = StdLib.serialize(milestones);
        ByteString buyerData = StdLib.serialize(new WCABuyerInfo(maxTokenSoldCount));

        // store
        wcaBasicInfoMap.put(identifier, basicData);
        wcaMilestonesMap.put(identifier, milestoneData);
        wcaBuyerInfoMap.put(identifier, buyerData);
        
        // save identifier
        insertIdentifier(owner, identifier);

        // fire event and done
        onCreateWCA.fire(owner, identifier, milestones.size());
        return identifier;
    }

    public static void finishMilestone(String identifier, int index, String proofOfWork) throws Exception {
        WCABasicInfo basicInfo = getWCABasicInfo(identifier);
        require(basicInfo != null, "Identifier not found.");
        // only creator can update WCA to finished
        require(Runtime.checkWitness(basicInfo.owner) || basicInfo.owner == Runtime.getCallingScriptHash(),
                "Invalid caller signature. The caller needs to be the owner account.");
        require(basicInfo.paid, "You can't finish an unpaid WCA.");
        List<WCAMilestone> milestones = getWCAMilestones(identifier);
        require(basicInfo != null, "Identifier not found.");

        updateMilestone(basicInfo, milestones, index, proofOfWork);
        // store it back
        wcaBasicInfoMap.put(identifier, StdLib.serialize(basicInfo));

        onFinishMilestone.fire(identifier, index, proofOfWork);

        // if whole WCA is finished
        if (checkIfReadyToFinish(milestones)) {
            finishWCA(identifier);
        }
    }

    public static void finishWCA(String identifier) throws Exception {
        WCABasicInfo basicInfo = getWCABasicInfo(identifier);
        require(basicInfo != null, "Identifier not found.");
        require(basicInfo.paid, "You can not finish an unpaid WCA.");
        require(!basicInfo.finished, "You can not finish a WCA twice.");
        List<WCAMilestone> milestones = getWCAMilestones(identifier);
        require(milestones != null, "Identifier not found.");

        if (!Runtime.checkWitness(basicInfo.owner)) {
            // only owner can finish an unfinished WCA
            require(checkIfReadyToFinish(milestones), "You can only apply this to a ready-to-finish WCA.");
        }
        // get wca buyer info obj
        WCABuyerInfo buyerInfo = getWCABuyerInfo(identifier);
        require(buyerInfo != null, "Buyer info not found.");

        int remainTokens = basicInfo.getTotalStake() + buyerInfo.totalPurchasedAmount;
        int totalMiletones = basicInfo.milestoneCount;
        int unfinishedMilestones = totalMiletones - basicInfo.finishedCount;

        // for each buyer, return their token based on unfinished ms count
        // also remove stakes for that unfinished one
        var buyers = buyerInfo.purchases.keys();
        for (int i = 0; i < buyers.length; i++) {
            var purchaseAmount = buyerInfo.purchases.get(buyers[i]);
            var totalAmount = purchaseAmount + purchaseAmount * basicInfo.stakePer100Token / 100;
            var returnAmount = totalAmount * unfinishedMilestones / totalMiletones;
            transferTokenTo(buyers[i], returnAmount, identifier);
            remainTokens -= returnAmount;
        }
        // considering all dicimals are floored, so totalTokens > 0
        // return the reset of total tokens to creator
        if (remainTokens > 0) {
            transferTokenTo(basicInfo.owner, remainTokens, identifier);
        }
        basicInfo.finished = true;
        // store it back
        wcaBasicInfoMap.put(identifier, StdLib.serialize(basicInfo));

        onFinishWCA.fire(identifier);
    }

    public static void refund(String identifier, Hash160 buyer) throws Exception {
        require(buyer.isValid(), "Buyer address is not a valid address.");
        require(Runtime.checkWitness(buyer) || buyer == Runtime.getCallingScriptHash(),
                "Invalid sender signature. The buyer needs to be the signing account.");
        WCABasicInfo basicInfo = getWCABasicInfo(identifier);
        require(basicInfo != null, "Identifier not found.");
        require(basicInfo.paid, "You can not refund an unpaid WCA.");
        require(!basicInfo.finished, "You can not refund a finished WCA.");
        List<WCAMilestone> milestones = getWCAMilestones(identifier);
        require(milestones != null, "Identifier not found.");
        require(!checkIfReadyToFinish(milestones), "You can not refund a finished WCA.");
        WCABuyerInfo buyerInfo = getWCABuyerInfo(identifier);
        require(buyerInfo != null, "Identifier not found.");

        if (checkIfThresholdMet(basicInfo, milestones)) {
            // after the threshold
            Pair<Integer, Integer> buyerAndCreator = buyerInfo.partialRefund(basicInfo, buyer);
            transferTokenTo(buyer, buyerAndCreator.first, identifier);
            transferTokenTo(basicInfo.owner, buyerAndCreator.second, identifier);
            onRefund.fire(buyer, identifier, buyerAndCreator.first, buyerAndCreator.second);
        } else {
            // full refund
            int amount = buyerInfo.fullRefund(buyer);
            transferTokenTo(buyer, amount, identifier);
            onRefund.fire(buyer, identifier, amount, 0);
        }

        // update buyer info
        ByteString buyerData = StdLib.serialize(buyerInfo);
        wcaBuyerInfoMap.put(identifier, buyerData);
    }

    public static void update(ByteString script, String manifest) throws Exception {
        require(Runtime.checkWitness(OWNER), "The calling entity is not the owner of this contract.");
        if (script.length() == 0 && manifest.length() == 0) {
            throw new Exception("The new contract script and manifest must not be empty.");
        }
        ContractManagement.update(script, manifest);
    }

    @OnVerification
    public static boolean verify() throws Exception {
        require(Runtime.checkWitness(OWNER), "The calling entity is not the owner of this contract.");
        return true;
    }

    public static Hash160 contractOwner() {
        return OWNER;
    }

    // ---------- Auxiliary functions ----------
    // Currently due to neow3j/neow3j#601, they won't work if they are outside of this contract.
    private static void transferTokenTo(Hash160 target, int amount, String identifier) {
        Contract.call(CAT_TOKEN_HASH, "transfer", CallFlags.All,
                new Object[] { Runtime.getExecutingScriptHash(), target, amount, identifier });
    }

    private static List<String> queryIdentifiers(Hash160 owner) {
        var rawData = wcaIdentifierMap.get(owner.toByteString());
        if (rawData == null)
            return null;
        return (List<String>) StdLib.deserialize(rawData);
    }

    private static void insertIdentifier(Hash160 owner, String identifier) {
        List<String> identifiers = queryIdentifiers(owner);
        if (identifiers == null) {
            identifiers = new List<>();
        }
        identifiers.add(identifier);
        wcaIdentifierMap.put(owner.toByteString(), StdLib.serialize(identifiers));
    }

    private static WCABasicInfo getWCABasicInfo(String identifier) {
        ByteString data = wcaBasicInfoMap.get(identifier);
        if (data == null) {
            return null;
        }
        return (WCABasicInfo) StdLib.deserialize(data);
    }

    private static WCABuyerInfo getWCABuyerInfo(String identifier) {
        ByteString data = wcaBuyerInfoMap.get(identifier);
        if (data == null) {
            return null;
        }
        return (WCABuyerInfo) StdLib.deserialize(data);
    }

    private static List<WCAMilestone> getWCAMilestones(String identifier) {
        ByteString data = wcaMilestonesMap.get(identifier);
        if (data == null) {
            return null;
        }
        return (List<WCAMilestone>) StdLib.deserialize(data);
    }
}
