package info.skyblond.nekohit.neo.contract;

import static io.neow3j.devpack.StringLiteralHelper.hexToBytes;
import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;
import info.skyblond.nekohit.neo.domain.WCABasicInfo;
import info.skyblond.nekohit.neo.domain.WCABuyerInfo;
import info.skyblond.nekohit.neo.domain.WCAMilestone;
import info.skyblond.nekohit.neo.domain.WCAPojo;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.CallFlags;
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
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event2Args;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event4Args;

/**
 * TODO This Contract just a prototype. Not tested yet.
 * 
 */
@ManifestExtra(key = "name", value = "WCA Contract")
@ManifestExtra(key = "github", value = "https://github.com/NekoHitDev/Ritmin")
@ManifestExtra(key = "author", value = "Something")
public class WCAContract {

    // refers to the ContractOwner wallet defined in `devnet.neo-express`
    // for private test net only
    private static final Hash160 OWNER = addressToScriptHash("NVCqzVkjApBWtgKa7c7gbURrJ4dmFYLekS");

    private static final StorageContext CTX = Storage.getStorageContext();

    // Note this is the reverse(the little endian) of CatToken Hash.
    private static final Hash160 CAT_TOKEN_HASH = new Hash160(hexToBytes("a72f9f85454c565f45d4141df0a4a3babb2655df"));

    // ---------- TODO Events below ----------
    @DisplayName("CreateWCA")
    private static Event4Args<Hash160, Integer, Integer, String> onCreateWCA;

    @DisplayName("BuyWCA")
    private static Event3Args<Hash160, String, Integer> onBuyWCA;

    @DisplayName("FinishWCA")
    private static Event2Args<String, Boolean> onFinishWCA;

    @DisplayName("onPayment")
    static Event3Args<Hash160, Integer, Object> onPayment;
    // ---------- TODO Events end ----------

    private static final StorageMap wcaBasicInfoMap = CTX.createMap("WCA_BASIC_INFO");

    private static final StorageMap wcaBuyerInfoMap = CTX.createMap("WCA_BUYER_INFO");

    private static final StorageMap wcaIdentifierMap = CTX.createMap("WCA_IDENTIFIERS");

    @OnNEP17Payment
    public static void onPayment(Hash160 from, int amount, Object data) throws Exception {
        if (CAT_TOKEN_HASH != Runtime.getCallingScriptHash()) {
            throw new Exception("Only Cat Token can invoke this function.");
        }

        var trueId = (String) data;
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            throw new Exception("Identifier not found.");
        }

        if (basicInfo.owner.equals(from)) {
            // owner paying stake
            if (basicInfo.paid)
                throw new Exception("You can't pay a paid WCA.");
            if (basicInfo.isFinished())
                throw new Exception("You can't pay a finished WCA.");
            if (basicInfo.getTotalStake() != amount)
                throw new Exception("Amount not correct");
            // not paid before, not finished, amount is correct
            basicInfo.paid = true;
            wcaBasicInfoMap.put(trueId, StdLib.serialize(basicInfo));
        } else {
            // buyer want to buy a WCA
            basicInfo.throwIfNotAvailableToBuy();
            WCABuyerInfo buyerInfo = getWCABuyerInfo(trueId);
            if (buyerInfo == null) {
                throw new Exception("Buyer info not found.");
            }
            buyerInfo.throwIfNotAvailableToBuy(amount);
            buyerInfo.addBuyer(from, amount);
            wcaBuyerInfoMap.put(trueId, StdLib.serialize(buyerInfo));
            onBuyWCA.fire(from, trueId, amount);
        }

        onPayment.fire(from, amount, data);
    }

    private static WCABasicInfo getWCABasicInfo(String trueId) {
        ByteString data = wcaBasicInfoMap.get(trueId);
        if (data == null) {
            return null;
        }
        return (WCABasicInfo) StdLib.deserialize(data);
    }

    private static WCABuyerInfo getWCABuyerInfo(String trueId) {
        ByteString data = wcaBuyerInfoMap.get(trueId);
        if (data == null) {
            return null;
        }
        return (WCABuyerInfo) StdLib.deserialize(data);
    }

    private static List<String> queryIdentifiers(Hash160 owner) {
        var rawData = wcaIdentifierMap.get(owner.asByteString());
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
        wcaIdentifierMap.put(owner.asByteString(), StdLib.serialize(identifiers));
    }

    private static void removeIdentifier(Hash160 owner, String identifier) {
        List<String> identifiers = queryIdentifiers(owner);
        if (identifiers == null) {
            // no identifiers
            return;
        }
        // find the index of the given id
        var index = 0;
        for (; index < identifiers.size(); index++) {
            if (identifiers.get(index).equals(identifier))
                break;
        }
        if (index == identifiers.size()) {
            // not found
            return;
        }
        identifiers.remove(index);
        // put it back
        wcaIdentifierMap.put(owner.asByteString(), StdLib.serialize(identifiers));
    }

    public static String queryWCA(String trueId) {
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            return "";
        }

        WCABuyerInfo buyerInfo = getWCABuyerInfo(trueId);
        if (buyerInfo == null) {
            return "";
        }

        WCAPojo result = new WCAPojo(basicInfo, buyerInfo);

        return StdLib.jsonSerialize(result);
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
     * @param identifier        the name of this WCA
     * 
     * @return the global unique id for this WCA
     */
    public static String createWCA(Hash160 owner, int stakePer100Token, int maxTokenSoldCount, String[] descriptions,
            int[] endTimestamps, String identifier) throws Exception {
        if (!owner.isValid()) {
            throw new Exception("Owner address is not a valid address.");
        }
        if (stakePer100Token <= 0) {
            throw new Exception("The stake amount per 100 token was non-positive.");
        }
        if (maxTokenSoldCount <= 0) {
            throw new Exception("The max sell token count was non-positive.");
        }
        if (!Runtime.checkWitness(owner) && owner != Runtime.getCallingScriptHash()) {
            throw new Exception("Invalid sender signature. The owner of the wca needs to be " + "the signing account.");
        }
        // identifier should be unique
        if (wcaBasicInfoMap.get(identifier) != null) {
            throw new Exception("Duplicate identifier.");
        }

        // check milestone
        if (descriptions.length != endTimestamps.length) {
            throw new Exception("Cannot decide milestones count.");
        }
        // convert to object on the fly, TODO check the gas cost
        List<WCAMilestone> milestones = new List<>();
        int lastTimestamp = 0;
        for (int i = 0; i < endTimestamps.length; i++) {
            if (lastTimestamp >= endTimestamps[i]) {
                throw new Exception("The end timestamp should increase.");
            }
            if (endTimestamps[i] <= Runtime.getTime()) {
                throw new Exception("The end timestamp is already expired.");
            }
            lastTimestamp = endTimestamps[i];
            milestones.add(new WCAMilestone(descriptions[i], endTimestamps[i]));
        }

        // save identifier
        insertIdentifier(owner, identifier);

        // create wca info obj
        WCABasicInfo info = new WCABasicInfo(owner, stakePer100Token, maxTokenSoldCount, milestones);
        ByteString basicData = StdLib.serialize(info);
        ByteString buyerData = StdLib.serialize(new WCABuyerInfo(maxTokenSoldCount));

        // store
        wcaBasicInfoMap.put(identifier, basicData);
        wcaBuyerInfoMap.put(identifier, buyerData);
        // fire event and done
        onCreateWCA.fire(owner, stakePer100Token, maxTokenSoldCount, identifier);
        return identifier;
    }

    public static void finishMilestone(String trueId, int index, String proofOfWork) throws Exception {
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            throw new Exception("Identifier not found.");
        }
        // only creator can update WCA to finished
        if (!Runtime.checkWitness(basicInfo.owner) && basicInfo.owner != Runtime.getCallingScriptHash()) {
            throw new Exception("Invalid caller signature. The caller needs to be the owner account.");
        }
        if (!basicInfo.paid) {
            throw new Exception("You can't finish an unpaid WCA.");
        }

        basicInfo.finishMilestone(index, proofOfWork);
        // store it back
        wcaBasicInfoMap.put(trueId, StdLib.serialize(basicInfo));
        // if whole WCA is finished
        if (basicInfo.isFinished()) {
            finishWCA(trueId);
        }
    }

    public static void finishWCA(String trueId) throws Exception {
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            throw new Exception("Identifier not found.");
        }
        if (!basicInfo.isFinished()) {
            throw new Exception("You can only apply this to a ready-to-finish WCA.");
        }
        // get wca buyer info obj
        WCABuyerInfo buyerInfo = getWCABuyerInfo(trueId);
        if (buyerInfo == null) {
            throw new Exception("Buyer info not found.");
        }

        int remainTokens = basicInfo.getTotalStake() + buyerInfo.totalAmount - buyerInfo.remainTokenCount;
        int totalMiletones = basicInfo.milestones.size();
        int unfinishedMilestones = totalMiletones - basicInfo.countFinishedMilestone();
        
        // for each buyer, return their token based on unfinished ms count
        // also remove stakes for that unfinished one
        for (int i = 0; i < buyerInfo.buyer.size(); i++) {
            var purchaseAmount = buyerInfo.amount.get(i);
            var totalAmount = purchaseAmount + purchaseAmount * basicInfo.stakePer100Token / 100;
            var returnAmount = totalAmount * unfinishedMilestones / totalMiletones;
            transferTokenTo(buyerInfo.buyer.get(i), returnAmount, trueId);
            remainTokens -= returnAmount;
        }
        // considering all dicimals are floored, so totalTokens >= 0
        // return the reset of total tokens to creator
        if (remainTokens > 0) {
            transferTokenTo(basicInfo.owner, remainTokens, trueId);
        }

        // remove the identifier
        // considering user need to access the link to work after WCA is finished
        // we shouldn't delete it immediately
        // wcaBasicInfoMap.delete(trueId);
        // wcaBuyerInfoMap.delete(trueId);
        // removeIdentifier(basicInfo.owner, trueId);
        onFinishWCA.fire(trueId, true);
    }

    // ---------- TODO ABOVE ----------

    private static void transferTokenTo(Hash160 target, int amount, String identifier) {
        Contract.call(CAT_TOKEN_HASH, "transfer", CallFlags.ALL,
                new Object[] { Runtime.getExecutingScriptHash(), target, amount, identifier });
    }

    public static void update(ByteString script, String manifest) throws Exception {
        throwIfSignerIsNotOwner();
        if (script.length() == 0 && manifest.length() == 0) {
            throw new Exception("The new contract script and manifest must not be empty.");
        }
        ContractManagement.update(script, manifest);
    }

    /**
     * I don't know why, but set it to false make it work properly
     */
    @OnVerification
    public static boolean verify() throws Exception {
        return false;
    }

    public static Hash160 contractOwner() {
        return OWNER;
    }

    private static void throwIfSignerIsNotOwner() throws Exception {
        if (!Runtime.checkWitness(OWNER)) {
            throw new Exception("The calling entity is not the owner of this contract.");
        }
    }
}
