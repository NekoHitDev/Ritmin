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

    // Note this is the reverse(the little endian) of CatToken Hash.
    private static final Hash160 CAT_TOKEN_HASH = new Hash160(hexToBytes("a72f9f85454c565f45d4141df0a4a3babb2655df"));

    // ---------- TODO BELOW ----------

    @DisplayName("CreateWCA")
    private static Event5Args<Hash160, Integer, Integer, Integer, String> onCreateWCA;

    @DisplayName("BuyWCA")
    private static Event3Args<Hash160, String, Integer> onBuyWCA;

    @DisplayName("FinishWCA")
    private static Event2Args<String, Boolean> onFinishWCA;

    private static final StorageMap wcaBasicInfoMap = CTX.createMap("WCA_BASIC_INFO");

    private static final StorageMap wcaBuyerInfoMap = CTX.createMap("WCA_BUYER_INFO");

    private static final StorageMap wcaIdentifierMap = CTX.createMap("WCA_IDENTIFIERS");

    @DisplayName("onPayment")
    static Event3Args<Hash160, Integer, Object> onPayment;

    @OnNEP17Payment
    public static void onPayment(Hash160 from, int amount, Object data) throws Exception {
        if (CAT_TOKEN_HASH != Runtime.getCallingScriptHash()) {
            throw new Exception("Only Cat Token can invoke this function.");
        }

        var trueId = (String) data;
        // identifier should be unique
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            throw new Exception("Identifier not found.");
        }

        if (basicInfo.endTimestamp <= Runtime.getTime()) {
            throw new Exception("You can't pay for a terminated WCA.");
        }

        if (basicInfo.owner.equals(from)) {
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
            if (!basicInfo.paid) {
                throw new Exception("You can't buy an unpaid WCA.");
            }
            // buyer want to buy a WCA
            buyWCA(from, trueId, amount);
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

        WCAPojo result = new WCAPojo(basicInfo.stakePer100Token, basicInfo.maxTokenSoldCount,
                buyerInfo.remainTokenCount, buyerInfo.buyer.size(), basicInfo.endTimestamp, basicInfo.paid);

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

        // identifier should be unique
        if (wcaBasicInfoMap.get(identifier) != null) {
            throw new Exception("Duplicate identifier.");
        }

        // save identifier
        insertIdentifier(owner, identifier);

        // create wca info obj
        WCABasicInfo info = new WCABasicInfo(owner, stakePer100Token, maxTokenSoldCount, endTimestamp);
        ByteString basicData = StdLib.serialize(info);
        ByteString buyerData = StdLib.serialize(new WCABuyerInfo(maxTokenSoldCount));

        // store
        wcaBasicInfoMap.put(identifier, basicData);
        wcaBuyerInfoMap.put(identifier, buyerData);
        // fire event and done
        onCreateWCA.fire(owner, stakePer100Token, maxTokenSoldCount, endTimestamp, identifier);
        return identifier;
    }

    private static void buyWCA(Hash160 buyer, String trueId, int amount) throws Exception {
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

    public static boolean finishWCA(String trueId, boolean finished) throws Exception {
        WCABasicInfo basicInfo = getWCABasicInfo(trueId);
        if (basicInfo == null) {
            throw new Exception("Identifier not found.");
        }

        if (!basicInfo.paid) {
            // unpaid wca, just remove it
            wcaBasicInfoMap.delete(trueId);
            wcaBuyerInfoMap.delete(trueId);
            removeIdentifier(basicInfo.owner, trueId);
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
                    buyerInfo.totalAmount + basicInfo.stakePer100Token * basicInfo.maxTokenSoldCount / 100, trueId);

            wcaBasicInfoMap.delete(trueId);
            wcaBuyerInfoMap.delete(trueId);
            removeIdentifier(basicInfo.owner, trueId);
            onFinishWCA.fire(trueId, true);
        } else if (basicInfo.endTimestamp <= Runtime.getTime()) {
            // otherwise, if wca is end, return amount+stake to buyer
            // return remain stake to creator
            for (int i = 0; i < buyerInfo.buyer.size(); i++) {
                int stake = buyerInfo.amount.get(i) * basicInfo.stakePer100Token / 100;
                transferTokenTo(buyerInfo.buyer.get(i), buyerInfo.amount.get(i) + stake, trueId);
            }

            // return remaining stake
            if (buyerInfo.remainTokenCount > 0) {
                transferTokenTo(basicInfo.owner, buyerInfo.remainTokenCount * basicInfo.stakePer100Token / 100, trueId);
            }

            wcaBasicInfoMap.delete(trueId);
            wcaBuyerInfoMap.delete(trueId);
            removeIdentifier(basicInfo.owner, trueId);
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
