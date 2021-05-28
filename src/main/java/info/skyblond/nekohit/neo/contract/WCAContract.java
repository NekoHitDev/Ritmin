package info.skyblond.nekohit.neo.contract;

import static io.neow3j.devpack.StringLiteralHelper.hexToBytes;
import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;
import info.skyblond.nekohit.neo.domain.WCABasicInfo;
import info.skyblond.nekohit.neo.domain.WCABuyerInfo;
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
import io.neow3j.devpack.events.Event5Args;

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

    private static final Hash160 CAT_TOKEN_HASH = new Hash160(hexToBytes("0d3aeba74209d6460f2c5a83d75c70dbc153aaa2"));

    // ---------- TODO BELOW ----------

    @DisplayName("CreateWCA")
    private static Event5Args<Hash160, Integer, Integer, Integer, String> onCreateWCA;

    @DisplayName("BuyWCA")
    private static Event3Args<Hash160, ByteString, Integer> onBuyWCA;

    @DisplayName("FinishWCA")
    private static Event2Args<ByteString, Boolean> onFinishWCA;

    private static final StorageMap wcaBasicInfoMap = CTX.createMap("WCA_BASIC_INFO");

    private static final StorageMap wcaBuyerInfoMap = CTX.createMap("WCA_BUYER_INFO");

    private static final StorageMap wcaIdentifierMap = CTX.createMap("WCA_IDENTIFIERS");

    @DisplayName("onPayment")
    static Event3Args<Hash160, Integer, Object> onPayment;

    @OnNEP17Payment
    public static void onPayment(Hash160 from, int amount, Object[] data) throws Exception {
        var trueId = (ByteString) data[0];
        // identifier should be unique
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            throw new Exception("Identifier not found.");
        }

        if (basicInfo.finished) {
            throw new Exception("You can't pay for a finished WCA.");
        }

        if (basicInfo.endTimestamp <= Runtime.getTime()) {
            throw new Exception("You can't pay for a terminated WCA.");
        }

        if (basicInfo.owner == from) {
            // owner paying stake
            if (basicInfo.paid) {
                throw new Exception("You can't pay a paid WCA.");
            }
            // check amount
            if (basicInfo.stakePer100Token * basicInfo.maxTokenSoldCount / 100 != amount) {
                throw new Exception("Amount not correct");
            }
            basicInfo.paid = true;
            wcaBasicInfoMap.put(trueId, StdLib.serialize(basicInfo));
        } else {
            // buyer want to buy a WCA
            buyWCA(from, trueId, amount);
        }

        onPayment.fire(from, amount, data);
    }

    private static ByteString getTrueId(Hash160 owner, String identifier) {
        return owner.asByteString().concat(identifier);
    }

    private static WCABasicInfo getWCABasicInfo(ByteString trueId) {
        ByteString data = wcaBasicInfoMap.get(trueId);
        if (data == null) {
            return null;
        }
        return (WCABasicInfo) StdLib.deserialize(data);
    }

    private static WCABuyerInfo getWCABuyerInfo(ByteString trueId) {
        ByteString data = wcaBuyerInfoMap.get(trueId);
        if (data == null) {
            return null;
        }
        return (WCABuyerInfo) StdLib.deserialize(data);
    }

    private static List<String> queryIdentifiers(Hash160 owner) {
        return (List<String>) StdLib.deserialize(wcaIdentifierMap.get(owner.asByteString()));
    }

    private static void insertIdentifier(Hash160 owner, String identifier) {
        List<String> identifiers = queryIdentifiers(owner);
        identifiers.add(identifier);
        wcaIdentifierMap.put(owner.asByteString(), StdLib.serialize(identifiers));
    }

    public static String queryWCA(ByteString trueId) {
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            return "";
        }

        WCABuyerInfo buyerInfo = getWCABuyerInfo(trueId);
        if (buyerInfo == null) {
            return "";
        }

        WCAPojo result = new WCAPojo(basicInfo.stakePer100Token, basicInfo.maxTokenSoldCount,
                buyerInfo.remainTokenCount, basicInfo.endTimestamp, basicInfo.paid);

        return StdLib.jsonSerialize(result);
    }

    /**
     * Request to create a WCA with given params.
     * 
     * @param owner             ScriptHash of WCA owner, need to be the signer of
     *                          this tx
     * @param stakePer100Token  stake price for 1.00 token, represent in fraction
     * @param maxTokenSoldCount total tokens for buyer
     * @param endTimestamp      timestamp in ms indecate when this WCA end
     * @param identifier        the name of this WCA
     * 
     * @return the global unique id for this WCA
     * 
     *         TODO: We might need a POJO for this, or have a separate function to
     *         add milestone
     */
    public static String createWCA(Hash160 owner, int stakePer100Token, int maxTokenSoldCount, int endTimestamp,
            String identifier) throws Exception {
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
            throw new Exception("Invalid sender signature. The owner of the wca needs to be " + "the signing account.");
        }

        ByteString trueId = getTrueId(owner, identifier);
        // identifier should be unique
        if (wcaBasicInfoMap.get(trueId) != null) {
            throw new Exception("Duplicate identifier.");
        }
        // save identifier
        insertIdentifier(owner, identifier);

        // create wca info obj
        WCABasicInfo info = new WCABasicInfo(owner, stakePer100Token, maxTokenSoldCount, endTimestamp);
        ByteString basicData = StdLib.serialize(info);
        ByteString buyerData = StdLib.serialize(new WCABuyerInfo(maxTokenSoldCount));
        // store
        wcaBasicInfoMap.put(trueId, basicData);
        wcaBuyerInfoMap.put(trueId, buyerData);
        // fire event and done
        onCreateWCA.fire(owner, stakePer100Token, maxTokenSoldCount, endTimestamp, identifier);
        return trueId.toString();
    }

    public static void buyWCA(Hash160 buyer, ByteString trueId, int amount) throws Exception {
        if (!buyer.isValid()) {
            throw new Exception("Buyer address is not a valid address.");
        }
        if (amount <= 0) {
            throw new Exception("The token amount was non-positive.");
        }
        if (!Runtime.checkWitness(buyer) && buyer != Runtime.getCallingScriptHash()) {
            throw new Exception(
                    "Invalid sender signature. The sender of the tokens needs to be " + "the signing account.");
        }
        if (getTokenBalance(buyer) < amount) {
            throw new Exception("Insufficient account balance.");
        }

        // identifier should be unique
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            throw new Exception("Identifier not found.");
        }

        if (!basicInfo.paid) {
            throw new Exception("You can't buy an unpaid WCA.");
        }

        if (basicInfo.finished) {
            throw new Exception("You can't buy a finished WCA.");
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

        // store
        ByteString data = StdLib.serialize(buyerInfo);
        wcaBuyerInfoMap.put(trueId, data);
        onBuyWCA.fire(buyer, trueId, amount);
    }

    public static boolean finishWCA(ByteString trueId, boolean finished) throws Exception {
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            throw new Exception("Identifier not found.");
        }

        if (!basicInfo.paid) {
            // unpaid wca, just finished it
            basicInfo.finished = true;
            ByteString data = StdLib.serialize(basicInfo);
            wcaBasicInfoMap.put(trueId, data);
        }

        if (basicInfo.finished) {
            throw new Exception("You can't finish a finished WCA.");
        }

        if (finished) {
            // only creator can update WCA to finished
            if (!Runtime.checkWitness(basicInfo.owner) && basicInfo.owner != Runtime.getCallingScriptHash()) {
                throw new Exception("Invalid caller signature. The caller needs to be " + "the owner account.");
            }
            // otherwise anyone can request to check if wca is end
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
            transferTokenTo(basicInfo.owner,
                    buyerInfo.totalAmount + basicInfo.stakePer100Token * basicInfo.maxTokenSoldCount / 100);

            basicInfo.finished = true;
            ByteString data = StdLib.serialize(basicInfo);
            wcaBasicInfoMap.put(trueId, data);
            onFinishWCA.fire(trueId, true);
        } else if (basicInfo.endTimestamp <= Runtime.getTime()) {
            // otherwise, if wca is end, return amount+stake to buyer
            // return remain stake to creator
            for (int i = 0; i < buyerInfo.buyer.size(); i++) {
                int stake = buyerInfo.amount.get(i) * basicInfo.stakePer100Token / 100;
                transferTokenTo(buyerInfo.buyer.get(i), buyerInfo.amount.get(i) + stake);
            }

            // return remaining stake
            if (buyerInfo.remainTokenCount > 0) {
                transferTokenTo(basicInfo.owner, buyerInfo.remainTokenCount * basicInfo.stakePer100Token / 100);
            }

            basicInfo.finished = true;
            ByteString data = StdLib.serialize(basicInfo);
            wcaBasicInfoMap.put(trueId, data);
            onFinishWCA.fire(trueId, true);
        } else {
            // nothing is done.
            return false;
        }

        return true;
    }

    // ---------- TODO ABOVE ----------

    private static int getTokenBalance(Hash160 address) {
        return (int) Contract.call(CAT_TOKEN_HASH, "balanceOf", CallFlags.ALL, new Object[] { address });
    }

    private static void transferTokenTo(Hash160 target, int amount) {
        Contract.call(CAT_TOKEN_HASH, "transfer", CallFlags.ALL,
                new Object[] { Runtime.getExecutingScriptHash(), target, amount, null });
    }

    public static void update(ByteString script, String manifest) throws Exception {
        throwIfSignerIsNotOwner();
        if (script.length() == 0 && manifest.length() == 0) {
            throw new Exception("The new contract script and manifest must not be empty.");
        }
        ContractManagement.update(script, manifest);
    }

    @OnVerification
    public static boolean verify() throws Exception {
        throwIfSignerIsNotOwner();
        return true;
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
