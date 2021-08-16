package info.skyblond.nekohit.neo.contract;

import info.skyblond.nekohit.neo.domain.*;
import info.skyblond.nekohit.neo.helper.Pair;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.*;
import io.neow3j.devpack.annotations.*;
import io.neow3j.devpack.constants.CallFlags;
import io.neow3j.devpack.constants.FindOptions;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event4Args;

import static info.skyblond.nekohit.neo.contract.WCAAuxiliary.*;
import static info.skyblond.nekohit.neo.helper.Utils.require;
import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;

@SuppressWarnings("unused")
@ManifestExtra(key = "name", value = "WCA Contract")
@ManifestExtra(key = "github", value = "https://github.com/NekoHitDev/Ritmin")
@ManifestExtra(key = "author", value = "NekoHitDev")
@Trust(value = "*")
@Permission(contract = "*")
public class WCAContract {
    // public net owner: NV5CSGyT6B39fZJ6zw4x6gh1b3C6cpjTm3
    // private net owner: NM9GZtomtwHRmqCkj7TgPMq5ssDnHsP7h5
    static final Hash160 OWNER = addressToScriptHash("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>");

    // address of CatToken Hash.
    // For private test net: NL75sYPVR5NcVpimsA4THSG6gwB9iNFPcQ
    // For public test net: NfbKv3Rg6grgkLVG7SJYtPmhJXcW43RzbH
    static final Hash160 CAT_TOKEN_HASH = addressToScriptHash("<CAT_TOKEN_CONTRACT_ADDRESS_PLACEHOLDER>");

    private static final StorageContext CTX = Storage.getStorageContext();
    private static final StorageMap wcaBasicInfoMap = CTX.createMap("BASIC_INFO");
    private static final StorageMap wcaMilestonesMap = CTX.createMap("MILESTONES");
    private static final StorageMap wcaBuyerInfoMap = CTX.createMap("BUYER_INFO");

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

    @DisplayName("CancelWCA")
    private static Event1Arg<String> onCancelWCA;


    @OnNEP17Payment
    public static void onPayment(Hash160 from, int amount, Object data) throws Exception {
        require(CAT_TOKEN_HASH == Runtime.getCallingScriptHash(), Messages.INVALID_CALLER);
        require(amount > 0, Messages.INVALID_AMOUNT);
        String identifier = (String) data;
        WCABasicInfo basicInfo = getWCABasicInfo(identifier);
        require(basicInfo != null, Messages.RECORD_NOT_FOUND);
        List<WCAMilestone> milestones = getWCAMilestones(identifier);
        require(milestones != null, Messages.BROKEN_RECORD);

        if (basicInfo.owner.equals(from)) {
            // owner paying stake
            require(basicInfo.status == 0, Messages.INVALID_STATUS_ALLOW_PENDING);
            require(basicInfo.getTotalStake() == amount, Messages.INCORRECT_AMOUNT);
            // unpaid before, amount is correct, set to OPEN
            basicInfo.status = 1;
            // update status in case the threshold milestone is expired
            basicInfo.updateStatus(milestones);
            updateWCABasicInfo(identifier, basicInfo);
            onPayWCA.fire(from, identifier, amount);
        } else {
            // buyer want to buy a WCA, update status first
            basicInfo.updateStatus(milestones);
            require(basicInfo.status == 1 || basicInfo.status == 2, Messages.INVALID_STATUS_ALLOW_OPEN_AND_ACTIVE);
            require(!checkIfReadyToFinish(milestones), Messages.INVALID_STATUS_READY_TO_FINISH);
            WCABuyerInfo buyerInfo = getWCABuyerInfo(identifier);
            require(buyerInfo != null, Messages.BROKEN_RECORD);
            buyerInfo.recordPurchase(from, amount);
            updateWCABasicInfo(identifier, basicInfo);
            updateWCABuyerInfo(identifier, buyerInfo);
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

        WCAPojo pojo = new WCAPojo(identifier, basicInfo, milestones, buyerInfo);
        return StdLib.jsonSerialize(pojo);
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

    public static String advanceQuery(
            Hash160 creator, Hash160 buyer, int page, int size
    ) throws Exception {
        require(page >= 1, Messages.INVALID_PAGE);
        require(size >= 1, Messages.INVALID_SIZE);
        int offset = (page - 1) * size;
        int count = 0;
        List<WCAPojo> result = new List<>();
        Iterator<Iterator.Struct<ByteString, ByteString>> iter = Storage.find(CTX, "BASIC_INFO", FindOptions.RemovePrefix);
        while (result.size() < size && iter.next()) {
            String identifier = iter.get().key.toString();
            WCABasicInfo basicInfo = getWCABasicInfo(identifier);
            List<WCAMilestone> milestonesInfo = getWCAMilestones(identifier);
            WCABuyerInfo buyerInfo = getWCABuyerInfo(identifier);

            if (basicInfo == null || milestonesInfo == null || buyerInfo == null) {
                continue;
            }

            if (!basicInfo.bePublic) {
                continue;
            }

            if (creator != null && creator != Hash160.zero()) {
                // filter creator
                if (basicInfo.owner != creator) {
                    continue;
                }
            }
            if (buyer != null && buyer != Hash160.zero()) {
                // filter buyer
                if (!buyerInfo.purchases.containsKey(buyer)) {
                    continue;
                }
            }
            WCAPojo pojo = new WCAPojo(identifier, basicInfo, milestonesInfo, buyerInfo);
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
        require(Runtime.checkWitness(owner) || owner == Runtime.getCallingScriptHash(), Messages.INVALID_SIGNATURE);
        // identifier should be unique
        require(wcaBasicInfoMap.get(identifier) == null, Messages.DUPLICATED_ID);
        // check milestone
        require(milestoneTitles.length == milestoneDescriptions.length, Messages.INVALID_MILESTONES_COUNT);
        require(milestoneDescriptions.length == endTimestamps.length, Messages.INVALID_MILESTONES_COUNT);

        // convert to object on the fly
        List<WCAMilestone> milestones = new List<>();
        int lastTimestamp = 0;
        for (int i = 0; i < endTimestamps.length; i++) {
            require(lastTimestamp < endTimestamps[i], Messages.INVALID_TIMESTAMP);
            require(endTimestamps[i] > Runtime.getTime(), Messages.EXPIRED_TIMESTAMP);
            lastTimestamp = endTimestamps[i];
            milestones.add(new WCAMilestone(milestoneTitles[i], milestoneDescriptions[i], endTimestamps[i]));
        }

        // create wca info obj
        WCABasicInfo basicInfo = new WCABasicInfo(
                owner, wcaDescription, stakePer100Token, maxTokenSoldCount,
                milestones.size(), thresholdIndex, coolDownInterval, bePublic
        );
        // store
        updateWCABasicInfo(identifier, basicInfo);
        updateWCABuyerInfo(identifier, new WCABuyerInfo(maxTokenSoldCount));
        updateWCAMilestones(identifier, milestones);
        // fire event and done
        onCreateWCA.fire(owner, identifier, milestones.size());
        return identifier;
    }

    public static void finishMilestone(String identifier, int index, String proofOfWork) throws Exception {
        WCABasicInfo basicInfo = getWCABasicInfo(identifier);
        require(basicInfo != null, Messages.RECORD_NOT_FOUND);
        // only creator can update WCA to finished
        require(Runtime.checkWitness(basicInfo.owner) || basicInfo.owner == Runtime.getCallingScriptHash(), Messages.INVALID_SIGNATURE);
        require(basicInfo.status == 1 || basicInfo.status == 2, Messages.INVALID_STATUS_ALLOW_OPEN_AND_ACTIVE);
        List<WCAMilestone> milestones = getWCAMilestones(identifier);
        require(milestones != null, Messages.BROKEN_RECORD);

        updateMilestone(basicInfo, milestones, index, proofOfWork);

        // store it back
        updateWCABasicInfo(identifier, basicInfo);
        updateWCAMilestones(identifier, milestones);
        onFinishMilestone.fire(identifier, index, proofOfWork);

        // if whole WCA is finished
        if (checkIfReadyToFinish(milestones)) {
            finishWCA(identifier);
        }
    }

    public static void finishWCA(String identifier) throws Exception {
        WCABasicInfo basicInfo = getWCABasicInfo(identifier);
        require(basicInfo != null, Messages.RECORD_NOT_FOUND);
        require(basicInfo.status == 1 || basicInfo.status == 2, Messages.INVALID_STATUS_ALLOW_OPEN_AND_ACTIVE);
        List<WCAMilestone> milestones = getWCAMilestones(identifier);
        require(milestones != null, Messages.BROKEN_RECORD);

        if (!Runtime.checkWitness(basicInfo.owner)) {
            // only owner can finish an unfinished WCA
            require(checkIfReadyToFinish(milestones), Messages.INVALID_STATUS_ALLOW_READY_TO_FINISH);
        }
        // get wca buyer info obj
        WCABuyerInfo buyerInfo = getWCABuyerInfo(identifier);
        require(buyerInfo != null, Messages.BROKEN_RECORD);

        int remainTokens = basicInfo.getTotalStake() + buyerInfo.totalPurchasedAmount;
        int totalMilestones = basicInfo.milestoneCount;
        int unfinishedMilestones = totalMilestones - basicInfo.finishedCount;

        // for each buyer, return their token based on unfinished ms count
        // also remove stakes for that unfinished one
        Hash160[] buyers = buyerInfo.purchases.keys();
        for (Hash160 buyer : buyers) {
            int purchaseAmount = buyerInfo.purchases.get(buyer);
            int totalAmount = purchaseAmount + purchaseAmount * basicInfo.stakePer100Token / 100;
            int returnAmount = totalAmount * unfinishedMilestones / totalMilestones;
            transferTokenTo(buyer, returnAmount, identifier);
            remainTokens -= returnAmount;
        }
        // considering all decimals are floored, so totalTokens > 0
        // return the reset of total tokens to creator
        if (remainTokens > 0) {
            transferTokenTo(basicInfo.owner, remainTokens, identifier);
        }
        basicInfo.status = 3;
        basicInfo.lastUpdateTime = Runtime.getTime();
        // store it back
        updateWCABasicInfo(identifier, basicInfo);
        onFinishWCA.fire(identifier);
    }

    public static void refund(String identifier, Hash160 buyer) throws Exception {
        require(Hash160.isValid(buyer), Messages.INVALID_HASH160);
        require(Runtime.checkWitness(buyer) || buyer == Runtime.getCallingScriptHash(), Messages.INVALID_SIGNATURE);
        WCABasicInfo basicInfo = getWCABasicInfo(identifier);
        List<WCAMilestone> milestones = getWCAMilestones(identifier);
        WCABuyerInfo buyerInfo = getWCABuyerInfo(identifier);
        require(basicInfo != null, Messages.RECORD_NOT_FOUND);
        require(basicInfo.status == 1 || basicInfo.status == 2, Messages.INVALID_STATUS_ALLOW_OPEN_AND_ACTIVE);
        require(milestones != null, Messages.BROKEN_RECORD);
        require(!checkIfReadyToFinish(milestones), Messages.INVALID_STATUS_READY_TO_FINISH);
        require(buyerInfo != null, Messages.BROKEN_RECORD);

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
        updateWCABuyerInfo(identifier, buyerInfo);
    }

    public static void cancelWCA(String identifier) throws Exception {
        // get obj
        WCABasicInfo basicInfo = getWCABasicInfo(identifier);
        require(basicInfo != null, Messages.RECORD_NOT_FOUND);
        List<WCAMilestone> milestones = getWCAMilestones(identifier);
        require(milestones != null, Messages.BROKEN_RECORD);
        WCABuyerInfo buyerInfo = getWCABuyerInfo(identifier);
        require(buyerInfo != null, Messages.BROKEN_RECORD);
        // check signature
        require(Hash160.isValid(basicInfo.owner), Messages.INVALID_HASH160);
        require(Runtime.checkWitness(basicInfo.owner) || basicInfo.owner == Runtime.getCallingScriptHash(), Messages.INVALID_SIGNATURE);
        // check status
        basicInfo.updateStatus(milestones);
        switch (basicInfo.status) {
            case 0:
                // PENDING, nothing to do
                break;
            case 1:
                // OPEN, refund to everyone
                // to creator
                transferTokenTo(basicInfo.owner, basicInfo.getTotalStake(), identifier);
                // to buyers
                Hash160[] buyers = buyerInfo.purchases.keys();
                for (Hash160 buyer : buyers) {
                    transferTokenTo(buyer, buyerInfo.purchases.get(buyer), identifier);
                }
                break;
            default:
                throw new Exception(Messages.INVALID_STATUS_ALLOW_PENDING_AND_OPEN);
        }
        // delete this id
        wcaBasicInfoMap.delete(identifier);
        wcaBuyerInfoMap.delete(identifier);
        wcaMilestonesMap.delete(identifier);
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

    private static void updateWCABasicInfo(String identifier, WCABasicInfo data) {
        wcaBasicInfoMap.put(identifier, StdLib.serialize(data));
    }

    private static void updateWCABuyerInfo(String identifier, WCABuyerInfo data) {
        wcaBuyerInfoMap.put(identifier, StdLib.serialize(data));
    }

    private static void updateWCAMilestones(String identifier, List<WCAMilestone> data) {
        wcaMilestonesMap.put(identifier, StdLib.serialize(data));
    }
}
