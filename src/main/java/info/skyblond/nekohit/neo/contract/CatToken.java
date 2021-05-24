package info.skyblond.nekohit.neo.contract;

import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;

import io.neow3j.contract.Token;
import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.CallFlags;
import io.neow3j.devpack.Contract;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.OnVerification;
import io.neow3j.devpack.annotations.SupportedStandards;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.events.Event3Args;

@ManifestExtra(key = "name", value = "CAT Token Contract")
@ManifestExtra(key = "github", value = "https://github.com/NekoHitDev/Ritmin")
@ManifestExtra(key = "author", value = "NekoHitDev")
@SupportedStandards("NEP-17")
public class CatToken {

    // refers to the ContractOwner wallet defined in `devnet.neo-express`
    // for private test net only
    private static final Hash160 OWNER = addressToScriptHash("NVCqzVkjApBWtgKa7c7gbURrJ4dmFYLekS");

    @DisplayName("Transfer")
    private static Event3Args<Hash160, Hash160, Integer> onTransfer;

    private static final int INITIAL_SUPPLY = 10_000_000_00;
    private static final int DECIMALS = 2;
    private static final String ASSET_PREFIX = "asset";
    private static final String TOTAL_SUPPLY_KEY = "totalSupply";
    private static final String SYMBOL = "CAT";
    private static final StorageContext sc = Storage.getStorageContext();
    private static final StorageMap assetMap = sc.createMap(ASSET_PREFIX);

    public static String symbol() {
        return SYMBOL;
    }

    public static int decimals() {
        return DECIMALS;
    }

    public static int totalSupply() {
        return getTotalSupply();
    }

    static int getTotalSupply() {
        return Storage.get(sc, TOTAL_SUPPLY_KEY).toInteger();
    }

    public static boolean transfer(Hash160 from, Hash160 to, int amount, Object[] data) throws Exception {

        if (!from.isValid() || !to.isValid()) {
            throw new Exception("From or To address is not a valid address.");
        }
        if (amount < 0) {
            throw new Exception("The transfer amount was negative.");
        }
        if (!Runtime.checkWitness(from) && from != Runtime.getCallingScriptHash()) {
            throw new Exception("Invalid sender signature. The sender of the tokens needs to be the signing account.");
        }
        if (getBalance(from) < amount) {
            return false;
        }
        if (from != to && amount != 0) {
            deductFromBalance(from, amount);
            addToBalance(to, amount);
        }

        onTransfer.fire(from, to, amount);
        if (ContractManagement.getContract(to) != null) {
            Contract.call(to, "onNEP17Payment", CallFlags.ALL, new Object[] { from, amount, data });
        }

        return true;
    }

    public static int balanceOf(Hash160 account) throws Exception {
        if (!account.isValid()) {
            throw new Exception("Argument is not a valid address.");
        }
        return getBalance(account);
    }

    @OnDeployment
    public static void deploy(Object data, boolean update) throws Exception {
        if (!update) {
            if (getTotalSupply() > 0) {
                throw new Exception("Contract was already deployed.");
            }
            // Initialize supply
            Storage.put(sc, TOTAL_SUPPLY_KEY, INITIAL_SUPPLY);
            // And allocate all tokens to the contract owner.
            assetMap.put(OWNER.toByteArray(), INITIAL_SUPPLY);
        }
    }

    public static void update(ByteString script, String manifest) throws Exception {
        throwIfSignerIsNotOwner();
        if (script.length() == 0 && manifest.length() == 0) {
            throw new Exception("The new contract script and manifest must not be empty.");
        }
        ContractManagement.update(script, manifest);
    }

    public static void destroy() throws Exception {
        throwIfSignerIsNotOwner();
        ContractManagement.destroy();
    }

    @OnVerification
    public static boolean verify() throws Exception {
        throwIfSignerIsNotOwner();
        return true;
    }

    /**
     * Gets the address of the contract owner.
     *
     * @return the address of the contract owner.
     */
    public static Hash160 contractOwner() {
        return OWNER;
    }

    private static void throwIfSignerIsNotOwner() throws Exception {
        if (!Runtime.checkWitness(OWNER)) {
            throw new Exception("The calling entity is not the owner of this contract.");
        }
    }

    private static void addToBalance(Hash160 key, int value) {
        assetMap.put(key.toByteArray(), getBalance(key) + value);
    }

    private static void deductFromBalance(Hash160 key, int value) {
        int oldValue = getBalance(key);
        if (oldValue == value) {
            assetMap.delete(key.toByteArray());
        } else {
            assetMap.put(key.toByteArray(), oldValue - value);
        }
    }

    private static int getBalance(Hash160 key) {
        return assetMap.get(key.toByteArray()).toInteger();
    }

    /**
     * Issue more tokens, by adding more token to the owner and update total supply
     */
    public static boolean issueNewToken(int amount) throws Exception {
        throwIfSignerIsNotOwner();
        if (amount <= 0) {
            return false;
        }
        addToBalance(OWNER, amount);
        Storage.put(sc, TOTAL_SUPPLY_KEY, getTotalSupply() + amount);
        return true;
    }

    /**
     * Remove tokens from owner's account.
     * 
     */
    public static boolean destroyToken(int amount) throws Exception {
        throwIfSignerIsNotOwner();
        if (amount <= 0) {
            return false;
        }
        if (getBalance(OWNER) < amount) {
            return false;
        }
        deductFromBalance(OWNER, amount);
        Storage.put(sc, TOTAL_SUPPLY_KEY, getTotalSupply() - amount);
        return true;
    }
}
