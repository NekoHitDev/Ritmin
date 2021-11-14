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
@ManifestExtra(key = "version", value = "v1-RC1")
// CatToken::transfer
@Permission(contract = "*", methods = {"transfer"})
// ContractManagement::update
@Permission(contract = "0xfffdc93764dbaddd97c48f252a53ea4643faa3fd", methods = {"update"})
public class WCAContract {
    static final Hash160 OWNER = addressToScriptHash("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>");

    private static final StorageContext CTX = Storage.getStorageContext();

    private static final String COUNTER_KEY = "CK";
    private static final StorageMap projectIdentifierMap = CTX.createMap("ID");
    private static final StorageMap projectStaticContentMap = CTX.createMap("SC");
    private static final StorageMap projectDynamicContentMap = CTX.createMap("DC");
    private static final StorageMap projectPurchaseRecordMap = CTX.createMap("PR");
    private static final StorageMap projectMilestoneMap = CTX.createMap("MS");

    // creator, identifier, milestone count
    @DisplayName("DeclareProject")
    private static Event3Args<Hash160, String, Integer> onDeclareProject;

    // owner, identifier, amount
    @DisplayName("PayStake")
    private static Event3Args<Hash160, String, Integer> onPayStake;

    // buyer, identifier, deal amount
    @DisplayName("PurchaseProject")
    private static Event3Args<Hash160, String, Integer> onPurchaseProject;

    // identifier, milestone index
    @DisplayName("FinishMilestone")
    private static Event2Args<String, Integer> onFinishMilestone;

    // identifier
    @DisplayName("FinishProject")
    private static Event1Arg<String> onFinishProject;

    // buyer, identifier, return to buyer amount, return to creator amount
    @DisplayName("Refund")
    private static Event4Args<Hash160, String, Integer, Integer> onRefund;

    // identifier
    @DisplayName("CancelProject")
    private static Event1Arg<String> onCancelProject;


    @OnNEP17Payment
    public static void onPayment(Hash160 from, int amount, Object data) throws Exception {
        require(amount > 0, ExceptionMessages.INVALID_AMOUNT);
        String identifier = (String) data;
        ByteString projectId = getProjectId(identifier);

        ProjectStaticContent staticContent = getStaticContent(projectId);
        // Check from hash, must be the one chosen by creator
        require(staticContent.tokenHash == Runtime.getCallingScriptHash(), ExceptionMessages.INVALID_CALLER);
        ProjectDynamicContent dynamicContent = getDynamicContent(projectId);

        if (staticContent.owner.equals(from)) {
            // owner paying stake
            require(dynamicContent.status == 0, ExceptionMessages.INVALID_STATUS_ALLOW_PENDING);
            require(staticContent.getTotalStake() == amount, ExceptionMessages.INCORRECT_AMOUNT);
            // unpaid before, amount is correct, set to ONGOING
            dynamicContent.status = 1;
            onPayStake.fire(from, identifier, amount);
        } else {
            require(dynamicContent.status == 1, ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING);
            require(!checkIfReadyToFinish(staticContent, dynamicContent), ExceptionMessages.INVALID_STAGE_READY_TO_FINISH);
            require(dynamicContent.remainTokenCount >= amount, ExceptionMessages.INSUFFICIENT_AMOUNT_REMAIN);
            dynamicContent.remainTokenCount -= amount;
            dynamicContent.totalPurchasedAmount += amount;
            dynamicContent.buyerCounter++;
            // update purchase record
            ByteString purchaseId = projectId.concat(from.toByteString());
            Integer value = projectPurchaseRecordMap.getInteger(purchaseId);
            if (value == null) {
                value = 0;
            }
            value += amount;
            projectPurchaseRecordMap.put(purchaseId, value);
            onPurchaseProject.fire(from, identifier, amount);
        }
        updateDynamicContent(projectId, dynamicContent);
    }

    public static String queryProject(String identifier) {
        try {
            ByteString projectId = getProjectId(identifier);
            ProjectStaticContent staticContent = getStaticContent(projectId);
            ProjectDynamicContent dynamicContent = getDynamicContent(projectId);
            ProjectMilestone[] milestones = getMilestones(projectId, staticContent);

            ProjectPojo pojo = new ProjectPojo(identifier, staticContent, dynamicContent, milestones);
            return StdLib.jsonSerialize(pojo);
        } catch (Exception e) {
            return "";
        }
    }

    public static int queryPurchase(String identifier, Hash160 buyer) {
        try {
            ByteString projectId = getProjectId(identifier);
            Integer value = projectPurchaseRecordMap.getInteger(projectId.concat(buyer.toByteString()));
            return value == null ? 0 : value;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String advanceQuery(
            Hash160 token, Hash160 creator, Hash160 buyer, int page, int size
    ) throws Exception {
        require(page >= 1, ExceptionMessages.INVALID_PAGE);
        require(size >= 1, ExceptionMessages.INVALID_SIZE);
        int offset = (page - 1) * size;
        int count = 0;
        List<ProjectPojo> result = new List<>();
        Iterator<Iterator.Struct<ByteString, ByteString>> iter = Storage.find(CTX, "ID", FindOptions.RemovePrefix);
        while (result.size() < size && iter.next()) {
            String identifier = iter.get().key.toString();
            ByteString projectId = iter.get().value;
            ProjectStaticContent staticContent = getStaticContent(projectId);
            ProjectMilestone[] milestonesInfo = getMilestones(projectId, staticContent);
            ProjectDynamicContent dynamicContent = getDynamicContent(projectId);

            if (!staticContent.bePublic) {
                continue;
            }

            if (token != null && token != Hash160.zero()) {
                // filter token
                if (staticContent.tokenHash != token) {
                    continue;
                }
            }
            if (creator != null && creator != Hash160.zero()) {
                // filter creator
                if (staticContent.owner != creator) {
                    continue;
                }
            }
            if (buyer != null && buyer != Hash160.zero()) {
                // filter buyer
                if (projectPurchaseRecordMap.get(projectId.concat(buyer.toByteString())) == null) {
                    continue;
                }
            }
            ProjectPojo pojo = new ProjectPojo(identifier, staticContent, dynamicContent, milestonesInfo);
            if (count >= offset) {
                result.add(pojo);
            }
            count++;
        }
        return StdLib.jsonSerialize(result);
    }

    public static String declareProject(
            Hash160 owner, String projectDescription,
            Hash160 tokenHash, int stakeRate100, int maxTokenSoldCount,
            String[] milestoneTitles, String[] milestoneDescriptions, int[] endTimestamps,
            int thresholdIndex, int coolDownInterval,
            boolean bePublic, String identifier
    ) throws Exception {
        require(Hash160.isValid(owner), ExceptionMessages.INVALID_HASH160);
        require(Hash160.isValid(tokenHash), ExceptionMessages.INVALID_HASH160);
        require(Runtime.checkWitness(owner) || owner == Runtime.getCallingScriptHash(), ExceptionMessages.INVALID_SIGNATURE);
        // projectId should be unique
        require(identifier.length() != 0, ExceptionMessages.EMPTY_ID);
        require(projectIdentifierMap.get(identifier) == null, ExceptionMessages.DUPLICATED_ID);
        Integer counter = Storage.getInteger(CTX, COUNTER_KEY);
        if (counter == null) {
            counter = 0;
        }
        counter++; // update counter
        Storage.put(CTX, COUNTER_KEY, counter);
        // save project id
        ByteString projectId = Utils.intToByteString(counter);
        projectIdentifierMap.put(identifier, projectId);

        require(projectDescription != null, ExceptionMessages.NULL_DESCRIPTION);
        require(stakeRate100 > 0, ExceptionMessages.INVALID_STAKE_RATE);
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
            updateMilestone(projectId, i, new ProjectMilestone(milestoneTitles[i], milestoneDescriptions[i], t));
        }
        require(thresholdIndex >= 0 && thresholdIndex < milestoneCount, ExceptionMessages.INVALID_THRESHOLD_INDEX);
        require(coolDownInterval > 0, ExceptionMessages.INVALID_COOL_DOWN_INTERVAL);

        // create project info obj
        ProjectStaticContent staticContent = new ProjectStaticContent(
                owner, projectDescription, tokenHash, stakeRate100, maxTokenSoldCount,
                milestoneCount, thresholdIndex, coolDownInterval,
                endTimestamps[thresholdIndex], endTimestamps[milestoneCount - 1],
                bePublic
        );

        // store
        projectStaticContentMap.put(projectId, StdLib.serialize(staticContent));
        updateDynamicContent(projectId, new ProjectDynamicContent(maxTokenSoldCount));
        // fire event and done
        onDeclareProject.fire(owner, identifier, milestoneTitles.length);
        return identifier;
    }

    public static void finishMilestone(String identifier, int index, String proofOfWork) throws Exception {
        ByteString projectId = getProjectId(identifier);
        ProjectStaticContent staticContent = getStaticContent(projectId);
        // only creator can update project to finished
        require(Runtime.checkWitness(staticContent.owner) || staticContent.owner == Runtime.getCallingScriptHash(), ExceptionMessages.INVALID_SIGNATURE);
        ProjectDynamicContent dynamicContent = getDynamicContent(projectId);
        require(dynamicContent.status == 1, ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING);
        ProjectMilestone ms = getMilestone(projectId, index);
        require(ms != null, ExceptionMessages.RECORD_NOT_FOUND);
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
        if (index >= staticContent.thresholdIndex) {
            // in case creator skip the threshold milestone
            dynamicContent.thresholdMilestoneFinished = true;
        }
        if (index == staticContent.milestoneCount - 1) {
            dynamicContent.lastMilestoneFinished = true;
        }

        // store it back
        updateDynamicContent(projectId, dynamicContent);
        updateMilestone(projectId, index, ms);
        onFinishMilestone.fire(identifier, index);

        // if whole project is finished
        if (checkIfReadyToFinish(staticContent, dynamicContent)) {
            finishProject(identifier);
        }
    }

    public static void finishProject(String identifier) throws Exception {
        ByteString projectId = getProjectId(identifier);
        ProjectStaticContent staticContent = getStaticContent(projectId);
        ProjectDynamicContent dynamicContent = getDynamicContent(projectId);
        require(dynamicContent.status == 1, ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING);

        if (!Runtime.checkWitness(staticContent.owner)) {
            // only owner can finish an unfinished project
            require(checkIfReadyToFinish(staticContent, dynamicContent), ExceptionMessages.INVALID_STAGE_ALLOW_READY_TO_FINISH);
        }

        // Update status first to prevent re-entry attack
        dynamicContent.status = 2;
        dynamicContent.lastUpdateTime = Runtime.getTime();
        updateDynamicContent(projectId, dynamicContent);
        // At this time, the project is finished, no more operation is allowed

        int remainTokens = staticContent.getTotalStake() + dynamicContent.totalPurchasedAmount;
        int totalMilestones = staticContent.milestoneCount;
        int unfinishedMilestones = totalMilestones - dynamicContent.finishedMilestoneCount;

        // If there are unfinished milestone, refund
        if (unfinishedMilestones != 0) {
            // for each buyer, return their token based on unfinished ms count
            // also remove stakes for that unfinished one
            ByteString prefix = new ByteString("PR").concat(projectId);
            Iterator<Iterator.Struct<ByteString, ByteString>> iter = Storage.find(CTX, prefix, FindOptions.RemovePrefix);
            while (iter.next()) {
                Iterator.Struct<ByteString, ByteString> elem = iter.get();
                Hash160 buyer = new Hash160(elem.key);
                int purchaseAmount = elem.value.toIntOrZero();
                int totalAmount = purchaseAmount + purchaseAmount * staticContent.stakeRate100 / 100;
                int returnAmount = totalAmount * unfinishedMilestones / totalMilestones;
                transferTokenTo(staticContent.tokenHash, buyer, returnAmount, identifier);
                remainTokens -= returnAmount;
            }
        }
        // considering all decimals are floored, so totalTokens > 0
        // return the reset of total tokens to creator
        if (remainTokens > 0) {
            transferTokenTo(staticContent.tokenHash, staticContent.owner, remainTokens, identifier);
        }
        onFinishProject.fire(identifier);
    }

    public static void refund(String identifier, Hash160 buyer) throws Exception {
        require(Hash160.isValid(buyer), ExceptionMessages.INVALID_HASH160);
        require(Runtime.checkWitness(buyer) || buyer == Runtime.getCallingScriptHash(), ExceptionMessages.INVALID_SIGNATURE);
        ByteString projectId = getProjectId(identifier);
        ProjectStaticContent staticContent = getStaticContent(projectId);
        ProjectDynamicContent dynamicContent = getDynamicContent(projectId);
        require(dynamicContent.status == 1, ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING);

        require(!checkIfReadyToFinish(staticContent, dynamicContent), ExceptionMessages.INVALID_STAGE_READY_TO_FINISH);
        ByteString purchaseId = projectId.concat(buyer.toByteString());
        Integer value = projectPurchaseRecordMap.getInteger(purchaseId);
        require(value != null && value > 0, ExceptionMessages.RECORD_NOT_FOUND);
        // After get the purchase record, delete it.
        // Re-entry attack will get record not found exception at next call.
        projectPurchaseRecordMap.delete(purchaseId);

        if (checkIfThresholdMet(staticContent, dynamicContent)) {
            // after the threshold
            Pair<Integer, Integer> buyerAndCreator = dynamicContent.partialRefund(staticContent, value);
            transferTokenTo(staticContent.tokenHash, buyer, buyerAndCreator.first, identifier);
            transferTokenTo(staticContent.tokenHash, staticContent.owner, buyerAndCreator.second, identifier);
            onRefund.fire(buyer, identifier, buyerAndCreator.first, buyerAndCreator.second);
        } else {
            // full refund
            int amount = dynamicContent.fullRefund(value);
            transferTokenTo(staticContent.tokenHash, buyer, amount, identifier);
            onRefund.fire(buyer, identifier, amount, 0);
        }
        // update buyer info
        updateDynamicContent(projectId, dynamicContent);
    }

    public static void cancelProject(String identifier) throws Exception {
        ByteString projectId = getProjectId(identifier);
        // Delete project id. Re-entry attack will fail since the id has been deleted.
        projectIdentifierMap.delete(identifier);
        // get obj
        ProjectStaticContent staticContent = getStaticContent(projectId);
        ProjectDynamicContent dynamicContent = getDynamicContent(projectId);

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
                transferTokenTo(staticContent.tokenHash, staticContent.owner, staticContent.getTotalStake(), identifier);
                // to buyers
                ByteString prefix = new ByteString("PR").concat(projectId);
                Iterator<Iterator.Struct<ByteString, ByteString>> iter = Storage.find(CTX, prefix, FindOptions.RemovePrefix);
                while (iter.next()) {
                    Iterator.Struct<ByteString, ByteString> elem = iter.get();
                    Hash160 buyer = new Hash160(elem.key);
                    int purchaseAmount = elem.value.toIntOrZero();
                    // delete record
                    projectPurchaseRecordMap.delete(projectId.concat(buyer.toByteString()));
                    transferTokenTo(staticContent.tokenHash, buyer, purchaseAmount, identifier);
                }
                break;
            default:
                // Cancel is not available for the rest of status
                throw new Exception(ExceptionMessages.INVALID_STATUS_ALLOW_PENDING_AND_ONGOING);
        }
        // delete this id
        projectStaticContentMap.delete(projectId);
        projectDynamicContentMap.delete(projectId);
        // delete milestones
        ByteString prefix = new ByteString("MS").concat(projectId);
        Iterator<ByteString> iter = Storage.find(CTX, prefix, FindOptions.KeysOnly);
        while (iter.next()) {
            Storage.delete(CTX, iter.get());
        }

        onCancelProject.fire(identifier);
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
    private static void transferTokenTo(Hash160 contract, Hash160 target, int amount, String identifier) {
        Contract.call(contract, "transfer", CallFlags.All,
                new Object[]{Runtime.getExecutingScriptHash(), target, amount, identifier});
    }

    /**
     * Check and get the id of the given identifier.
     * If identifier not exist, exception will be thrown.
     */
    private static ByteString getProjectId(String identifier) throws Exception {
        ByteString id = projectIdentifierMap.get(identifier);
        require(id != null, ExceptionMessages.RECORD_NOT_FOUND);
        return id;
    }

    private static ProjectStaticContent getStaticContent(ByteString projectId) {
        ByteString data = projectStaticContentMap.get(projectId);
        return (ProjectStaticContent) StdLib.deserialize(data);
    }

    private static ProjectDynamicContent getDynamicContent(ByteString projectId) {
        ByteString data = projectDynamicContentMap.get(projectId);
        return (ProjectDynamicContent) StdLib.deserialize(data);
    }

    private static ProjectMilestone getMilestone(ByteString projectId, int index) throws Exception {
        // Since projectId has no fixed length, thus milestone index must have fixed length
        // otherwise there will be [010][1010] = [0101][010]
        ByteString data = projectMilestoneMap.get(
                projectId.concat(
                        Utils.paddingByteString(Utils.intToByteString(index), 20)
                ));
        if (data == null) {
            return null;
        }
        return (ProjectMilestone) StdLib.deserialize(data);
    }

    private static ProjectMilestone[] getMilestones(ByteString projectId, ProjectStaticContent staticContent) throws Exception {
        ProjectMilestone[] result = new ProjectMilestone[staticContent.milestoneCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = getMilestone(projectId, i);
        }
        return result;
    }

    private static void updateDynamicContent(ByteString projectId, ProjectDynamicContent data) {
        projectDynamicContentMap.put(projectId, StdLib.serialize(data));
    }

    private static void updateMilestone(ByteString projectId, int index, ProjectMilestone data) throws Exception {
        projectMilestoneMap.put(
                projectId.concat(
                        Utils.paddingByteString(Utils.intToByteString(index), 20)
                ),
                StdLib.serialize(data)
        );
    }
}
