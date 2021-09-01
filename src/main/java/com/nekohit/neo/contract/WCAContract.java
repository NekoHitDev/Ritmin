package com.nekohit.neo.contract;

import com.nekohit.neo.domain.*;
import com.nekohit.neo.helper.Pair;
import com.nekohit.neo.helper.Utils;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.*;
import io.neow3j.devpack.annotations.*;
import io.neow3j.devpack.constants.CallFlags;
import io.neow3j.devpack.constants.FindOptions;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event4Args;

import static com.nekohit.neo.contract.WCAAuxiliary.checkIfReadyToFinish;
import static com.nekohit.neo.contract.WCAAuxiliary.checkIfThresholdMet;
import static com.nekohit.neo.helper.Utils.require;
import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;

@SuppressWarnings("unused")
@ManifestExtra(key = "name", value = "WCA Contract")
@ManifestExtra(key = "github", value = "https://github.com/NekoHitDev/Ritmin")
@ManifestExtra(key = "author", value = "NekoHitDev")
@Permission(contract = "<CAT_TOKEN_CONTRACT_HASH_PLACEHOLDER>", methods = {"transfer"})
public class WCAContract {
    static final Hash160 OWNER = addressToScriptHash("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>");

    // address of CatToken Hash.
    static final Hash160 CAT_TOKEN_HASH = addressToScriptHash("<CAT_TOKEN_CONTRACT_ADDRESS_PLACEHOLDER>");

    private static final StorageContext CTX = Storage.getStorageContext();

    private static final String COUNTER_KEY = "CK";
    private static final StorageMap wcaIdentifierMap = CTX.createMap("ID");
    private static final StorageMap wcaStaticContentMap = CTX.createMap("SC");
    private static final StorageMap wcaDynamicContentMap = CTX.createMap("DC");
    private static final StorageMap wcaPurchaseRecordMap = CTX.createMap("PR");
    private static final StorageMap wcaMilestoneMap = CTX.createMap("MS");

    // creator, identifier, milestone count
    @DisplayName("CreateWCA")
    private static Event3Args<Hash160, String, Integer> onCreateWCA;

    // owner, identifier, amount
    @DisplayName("PayWCA")
    private static Event3Args<Hash160, String, Integer> onPayWCA;

    // buyer, identifier, deal amount
    @DisplayName("BuyWCA")
    private static Event3Args<Hash160, String, Integer> onBuyWCA;

    // identifier, milestone index
    @DisplayName("FinishMilestone")
    private static Event2Args<String, Integer> onFinishMilestone;

    // identifier
    @DisplayName("FinishWCA")
    private static Event1Arg<String> onFinishWCA;

    // buyer, identifier, return to buyer amount, return to creator amount
    @DisplayName("Refund")
    private static Event4Args<Hash160, String, Integer, Integer> onRefund;

    // identifier
    @DisplayName("CancelWCA")
    private static Event1Arg<String> onCancelWCA;


    @OnNEP17Payment
    public static void onPayment(Hash160 from, int amount, Object data) throws Exception {
        require(CAT_TOKEN_HASH == Runtime.getCallingScriptHash(), ExceptionMessages.INVALID_CALLER);
        require(amount > 0, ExceptionMessages.INVALID_AMOUNT);
        String identifier = (String) data;
        ByteString wcaId = getWCAId(identifier);

        WCAStaticContent staticContent = getWCAStaticContent(wcaId);
        WCADynamicContent dynamicContent = getWCADynamicContent(wcaId);

        if (staticContent.owner.equals(from)) {
            // owner paying stake
            require(dynamicContent.status == 0, ExceptionMessages.INVALID_STATUS_ALLOW_PENDING);
            require(staticContent.getTotalStake() == amount, ExceptionMessages.INCORRECT_AMOUNT);
            // unpaid before, amount is correct, set to ONGOING
            dynamicContent.status = 1;
        } else {
            require(dynamicContent.status == 1, ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING);
            require(!checkIfReadyToFinish(staticContent, dynamicContent), ExceptionMessages.INVALID_STAGE_READY_TO_FINISH);
            require(dynamicContent.remainTokenCount >= amount, "Insufficient token remain in this WCA.");
            dynamicContent.remainTokenCount -= amount;
            dynamicContent.totalPurchasedAmount += amount;
            dynamicContent.buyerCounter++;
            // update purchase record
            ByteString purchaseId = wcaId.concat(from.toByteString());
            Integer value = wcaPurchaseRecordMap.getInteger(purchaseId);
            if (value == null) value = 0;
            value += amount;
            wcaPurchaseRecordMap.put(purchaseId, value);
        }
        updateWCADynamicContent(wcaId, dynamicContent);
        onBuyWCA.fire(from, identifier, amount);
    }

    public static String queryWCA(String identifier) {
        try {
            ByteString wcaId = getWCAId(identifier);
            WCAStaticContent staticContent = getWCAStaticContent(wcaId);
            WCADynamicContent dynamicContent = getWCADynamicContent(wcaId);
            WCAMilestone[] milestones = getWCAMilestones(wcaId, staticContent);

            WCAPojo pojo = new WCAPojo(identifier, staticContent, dynamicContent, milestones);
            return StdLib.jsonSerialize(pojo);
        } catch (Exception e) {
            return "";
        }
    }

    public static int queryPurchase(String identifier, Hash160 buyer) {
        try {
            ByteString wcaId = getWCAId(identifier);
            Integer value = wcaPurchaseRecordMap.getInteger(wcaId.concat(buyer.toByteString()));
            return value == null ? 0 : value;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String advanceQuery(
            Hash160 creator, Hash160 buyer, int page, int size
    ) throws Exception {
        require(page >= 1, ExceptionMessages.INVALID_PAGE);
        require(size >= 1, ExceptionMessages.INVALID_SIZE);
        int offset = (page - 1) * size;
        int count = 0;
        List<WCAPojo> result = new List<>();
        Iterator<Iterator.Struct<ByteString, ByteString>> iter = Storage.find(CTX, "ID", FindOptions.RemovePrefix);
        while (result.size() < size && iter.next()) {
            String identifier = iter.get().key.toString();
            ByteString wcaId = iter.get().value;
            WCAStaticContent staticContent = getWCAStaticContent(wcaId);
            WCAMilestone[] milestonesInfo = getWCAMilestones(wcaId, staticContent);
            WCADynamicContent dynamicContent = getWCADynamicContent(wcaId);

            if (!staticContent.bePublic) {
                continue;
            }

            if (creator != null && creator != Hash160.zero()) {
                // filter creator
                if (staticContent.owner != creator) {
                    continue;
                }
            }
            if (buyer != null && buyer != Hash160.zero()) {
                // filter buyer
                if (wcaPurchaseRecordMap.get(wcaId.concat(buyer.toByteString())) == null) {
                    continue;
                }
            }
            WCAPojo pojo = new WCAPojo(identifier, staticContent, dynamicContent, milestonesInfo);
            if (count >= offset) {
                result.add(pojo);
            }
            count++;
        }
        return StdLib.jsonSerialize(result);
    }

    public static String createWCA(
            Hash160 owner, String wcaDescription,
            int stakePer100Token, int maxTokenSoldCount,
            String[] milestoneTitles, String[] milestoneDescriptions, int[] endTimestamps,
            int thresholdIndex, int coolDownInterval,
            boolean bePublic, String identifier
    ) throws Exception {
        require(Hash160.isValid(owner), ExceptionMessages.INVALID_HASH160);
        require(Runtime.checkWitness(owner) || owner == Runtime.getCallingScriptHash(), ExceptionMessages.INVALID_SIGNATURE);
        // wcaId should be unique
        require(wcaIdentifierMap.get(identifier) == null, ExceptionMessages.DUPLICATED_ID);
        Integer counter = Storage.getInteger(CTX, COUNTER_KEY);
        if (counter == null) counter = 0;
        wcaIdentifierMap.put(identifier, ++counter);
        Storage.put(CTX, COUNTER_KEY, counter);
        ByteString wcaId = Utils.intToByteString(counter);

        require(wcaDescription != null, ExceptionMessages.NULL_DESCRIPTION);
        require(stakePer100Token > 0, ExceptionMessages.INVALID_STAKE_RATE);
        require(maxTokenSoldCount > 0, ExceptionMessages.INVALID_MAX_SELL_AMOUNT);

        // check milestone
        int milestoneCount = endTimestamps.length;
        require(milestoneTitles.length == milestoneCount, ExceptionMessages.INVALID_MILESTONES_COUNT);
        require(milestoneCount == milestoneDescriptions.length, ExceptionMessages.INVALID_MILESTONES_COUNT);

        int lastTimestamp = 0;
        require(endTimestamps[0] > Runtime.getTime(), ExceptionMessages.EXPIRED_TIMESTAMP);
        for (int i = 0; i < milestoneCount; i++) {
            int t = endTimestamps[i];
            require(lastTimestamp < t, ExceptionMessages.INVALID_TIMESTAMP);
            lastTimestamp = t;
            updateMilestone(wcaId, i, new WCAMilestone(milestoneTitles[i], milestoneDescriptions[i], t));
        }
        require(thresholdIndex >= 0 && thresholdIndex < milestoneCount, ExceptionMessages.INVALID_THRESHOLD_INDEX);
        require(coolDownInterval > 0, ExceptionMessages.INVALID_COOL_DOWN_INTERVAL);

        // create wca info obj
        WCAStaticContent staticContent = new WCAStaticContent(
                owner, wcaDescription, stakePer100Token, maxTokenSoldCount,
                milestoneCount, thresholdIndex, coolDownInterval,
                endTimestamps[thresholdIndex], endTimestamps[milestoneCount - 1],
                bePublic
        );

        // store
        wcaStaticContentMap.put(wcaId, StdLib.serialize(staticContent));
        updateWCADynamicContent(wcaId, new WCADynamicContent(maxTokenSoldCount));
        // fire event and done
        onCreateWCA.fire(owner, identifier, milestoneTitles.length);
        return identifier;
    }

    public static void finishMilestone(String identifier, int index, String proofOfWork) throws Exception {
        ByteString wcaId = getWCAId(identifier);
        WCAStaticContent staticContent = getWCAStaticContent(wcaId);
        // only creator can update WCA to finished
        require(Runtime.checkWitness(staticContent.owner) || staticContent.owner == Runtime.getCallingScriptHash(), ExceptionMessages.INVALID_SIGNATURE);
        WCADynamicContent dynamicContent = getWCADynamicContent(wcaId);
        require(dynamicContent.status == 1, ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING);
        WCAMilestone ms = getWCAMilestone(wcaId, index);
        // check cool-down time first
        int currentTime = Runtime.getTime();
        require(dynamicContent.lastUpdateTime + staticContent.coolDownInterval <= currentTime, ExceptionMessages.COOL_DOWN_TIME_NOT_MET);
        require(index >= dynamicContent.nextMilestoneIndex, ExceptionMessages.INVALID_MILESTONE_PASSED);
        require(!ms.isFinished(), ExceptionMessages.INVALID_MILESTONE_FINISHED);
        require(!ms.isExpired(), ExceptionMessages.INVALID_MILESTONE_EXPIRED);
        // not finished nor expired, then we can modify it.
        require(proofOfWork != null && proofOfWork.length() != 0, ExceptionMessages.INVALID_PROOF_OF_WORK);
        ms.proofOfWork = proofOfWork;
        dynamicContent.nextMilestoneIndex = index + 1;
        dynamicContent.finishedMilestoneCount++;
        dynamicContent.lastUpdateTime = currentTime;
        if (index == staticContent.thresholdIndex) {
            dynamicContent.thresholdMilestoneFinished = true;
        }
        if (index == staticContent.milestoneCount - 1) {
            dynamicContent.lastMilestoneFinished = true;
        }

        // store it back
        updateWCADynamicContent(wcaId, dynamicContent);
        updateMilestone(wcaId, index, ms);
        onFinishMilestone.fire(identifier, index);

        // if whole WCA is finished
        if (checkIfReadyToFinish(staticContent, dynamicContent)) {
            finishWCA(identifier);
        }
    }

    public static void finishWCA(String identifier) throws Exception {
        ByteString wcaId = getWCAId(identifier);
        WCAStaticContent staticContent = getWCAStaticContent(wcaId);
        WCADynamicContent dynamicContent = getWCADynamicContent(wcaId);
        require(dynamicContent.status == 1, ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING);

        if (!Runtime.checkWitness(staticContent.owner)) {
            // only owner can finish an unfinished WCA
            require(checkIfReadyToFinish(staticContent, dynamicContent), ExceptionMessages.INVALID_STAGE_ALLOW_READY_TO_FINISH);
        }
        // get wca buyer info obj
        int remainTokens = staticContent.getTotalStake() + dynamicContent.totalPurchasedAmount;
        int totalMilestones = staticContent.milestoneCount;
        int unfinishedMilestones = totalMilestones - dynamicContent.finishedMilestoneCount;

        // for each buyer, return their token based on unfinished ms count
        // also remove stakes for that unfinished one
        ByteString prefix = new ByteString("PR").concat(wcaId);
        Iterator<Iterator.Struct<ByteString, ByteString>> iter = Storage.find(CTX, prefix, FindOptions.RemovePrefix);
        while (iter.next()) {
            Iterator.Struct<ByteString, ByteString> elem = iter.get();
            Hash160 buyer = new Hash160(elem.key);
            int purchaseAmount = elem.value.toInteger();
            int totalAmount = purchaseAmount + purchaseAmount * staticContent.stakePer100Token / 100;
            int returnAmount = totalAmount * unfinishedMilestones / totalMilestones;
            transferTokenTo(buyer, returnAmount, identifier);
            remainTokens -= returnAmount;
        }
        // considering all decimals are floored, so totalTokens > 0
        // return the reset of total tokens to creator
        if (remainTokens > 0) {
            transferTokenTo(staticContent.owner, remainTokens, identifier);
        }
        dynamicContent.status = 2;
        dynamicContent.lastUpdateTime = Runtime.getTime();
        // store it back
        updateWCADynamicContent(wcaId, dynamicContent);
        onFinishWCA.fire(identifier);
    }

    public static void refund(String identifier, Hash160 buyer) throws Exception {
        require(Hash160.isValid(buyer), ExceptionMessages.INVALID_HASH160);
        require(Runtime.checkWitness(buyer) || buyer == Runtime.getCallingScriptHash(), ExceptionMessages.INVALID_SIGNATURE);
        ByteString wcaId = getWCAId(identifier);
        WCAStaticContent staticContent = getWCAStaticContent(wcaId);
        WCADynamicContent dynamicContent = getWCADynamicContent(wcaId);
        require(dynamicContent.status == 1, ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING);

        require(!checkIfReadyToFinish(staticContent, dynamicContent), ExceptionMessages.INVALID_STAGE_READY_TO_FINISH);
        ByteString purchaseId = wcaId.concat(buyer.toByteString());
        Integer value = wcaPurchaseRecordMap.getInteger(purchaseId);
        if (value == null) value = 0;
        if (checkIfThresholdMet(staticContent, dynamicContent)) {
            // after the threshold
            Pair<Integer, Integer> buyerAndCreator = dynamicContent.partialRefund(staticContent, value);
            transferTokenTo(buyer, buyerAndCreator.first, identifier);
            transferTokenTo(staticContent.owner, buyerAndCreator.second, identifier);
            onRefund.fire(buyer, identifier, buyerAndCreator.first, buyerAndCreator.second);
        } else {
            // full refund
            int amount = dynamicContent.fullRefund(value);
            transferTokenTo(buyer, amount, identifier);
            onRefund.fire(buyer, identifier, amount, 0);
        }
        wcaPurchaseRecordMap.delete(purchaseId);
        // update buyer info
        updateWCADynamicContent(wcaId, dynamicContent);
    }

    public static void cancelWCA(String identifier) throws Exception {
        ByteString wcaId = getWCAId(identifier);
        // get obj
        WCAStaticContent staticContent = getWCAStaticContent(wcaId);
        WCADynamicContent dynamicContent = getWCADynamicContent(wcaId);

        // check signature
        require(Hash160.isValid(staticContent.owner), ExceptionMessages.INVALID_HASH160);
        require(Runtime.checkWitness(staticContent.owner) || staticContent.owner == Runtime.getCallingScriptHash(), ExceptionMessages.INVALID_SIGNATURE);
        // check status
        switch (dynamicContent.status) {
            case 0:
                // PENDING, nothing to do
                break;
            case 1:
                // ONGOING, check threshold
                require(!checkIfThresholdMet(staticContent, dynamicContent), ExceptionMessages.INVALID_STAGE_ACTIVE);
                // to creator
                transferTokenTo(staticContent.owner, staticContent.getTotalStake(), identifier);
                // to buyers
                ByteString prefix = new ByteString("PR").concat(wcaId);
                Iterator<Iterator.Struct<ByteString, ByteString>> iter = Storage.find(CTX, prefix, FindOptions.RemovePrefix);
                while (iter.next()) {
                    Iterator.Struct<ByteString, ByteString> elem = iter.get();
                    Hash160 buyer = new Hash160(elem.key);
                    int purchaseAmount = elem.value.toInteger();
                    // delete record
                    wcaPurchaseRecordMap.delete(wcaId.concat(buyer.toByteString()));
                    transferTokenTo(buyer, purchaseAmount, identifier);
                }
                break;
            default:
                // Cancel is not available for the rest of status
                throw new Exception(ExceptionMessages.INVALID_STATUS_ALLOW_PENDING_AND_ONGOING);
        }
        // delete this id
        wcaIdentifierMap.delete(identifier);
        wcaStaticContentMap.delete(wcaId);
        wcaDynamicContentMap.delete(wcaId);
        // delete milestones
        ByteString prefix = new ByteString("MS").concat(wcaId);
        Iterator<ByteString> iter = Storage.find(CTX, prefix, FindOptions.KeysOnly);
        while (iter.next()) {
            Storage.delete(CTX, iter.get());
        }

        onCancelWCA.fire(identifier);
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
    // Currently due to neow3j/neow3j#601, they won't work if they are outside this contract.
    private static void transferTokenTo(Hash160 target, int amount, String identifier) {
        Contract.call(CAT_TOKEN_HASH, "transfer", CallFlags.All,
                new Object[]{Runtime.getExecutingScriptHash(), target, amount, identifier});
    }

    /**
     * Check and get the wcaId of the given identifier.
     * If identifier not exist, exception will be thrown.
     */
    private static ByteString getWCAId(String identifier) throws Exception {
        ByteString id = wcaIdentifierMap.get(identifier);
        require(id != null, ExceptionMessages.RECORD_NOT_FOUND);
        return id;
    }

    private static WCAStaticContent getWCAStaticContent(ByteString wcaId) {
        ByteString data = wcaStaticContentMap.get(wcaId);
        return (WCAStaticContent) StdLib.deserialize(data);
    }

    private static WCADynamicContent getWCADynamicContent(ByteString wcaId) {
        ByteString data = wcaDynamicContentMap.get(wcaId);
        return (WCADynamicContent) StdLib.deserialize(data);
    }

    private static WCAMilestone getWCAMilestone(ByteString wcaId, int index) throws Exception {
        // Since wcaId has no fixed length, thus milestone index must have fixed length
        // otherwise there will be [010][1010] = [0101][010]
        ByteString data = wcaMilestoneMap.get(
                wcaId.concat(
                        Utils.paddingByteString(Utils.intToByteString(index), 20)
                ));
        return (WCAMilestone) StdLib.deserialize(data);
    }

    private static WCAMilestone[] getWCAMilestones(ByteString wcaId, WCAStaticContent staticContent) throws Exception {
        WCAMilestone[] result = new WCAMilestone[staticContent.milestoneCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = getWCAMilestone(wcaId, i);
        }
        return result;
    }

    private static void updateWCADynamicContent(ByteString wcaId, WCADynamicContent data) {
        wcaDynamicContentMap.put(wcaId, StdLib.serialize(data));
    }

    private static void updateMilestone(ByteString wcaId, int index, WCAMilestone data) throws Exception {
        wcaMilestoneMap.put(
                wcaId.concat(
                        Utils.paddingByteString(Utils.intToByteString(index), 20)
                ),
                StdLib.serialize(data)
        );
    }
}
