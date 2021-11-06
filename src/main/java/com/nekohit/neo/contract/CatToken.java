package com.nekohit.neo.contract;

import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.*;
import io.neow3j.devpack.annotations.*;
import io.neow3j.devpack.constants.CallFlags;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.FungibleToken;
import io.neow3j.devpack.events.Event2Args;
import io.neow3j.devpack.events.Event3Args;

import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;

@SuppressWarnings("unused")
@ManifestExtra(key = "name", value = "CAT Token")
@ManifestExtra(key = "github", value = "https://github.com/NekoHitDev/Ritmin")
@ManifestExtra(key = "author", value = "NekoHitDev")
@ManifestExtra(key = "version", value = "v1-RC1")
// Contract as receiver
@Permission(contract = "*", methods = "onNEP17Payment")
// USD token transfer
@Permission(contract = "<USD_TOKEN_CONTRACT_HASH_PLACEHOLDER>", methods = "transfer")
// ContractManagement::update
@Permission(contract = "0xfffdc93764dbaddd97c48f252a53ea4643faa3fd", methods = {"update"})
@SupportedStandards("NEP-17")
public class CatToken {
    private static final Hash160 OWNER = addressToScriptHash("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>");
    private static final Hash160 USD_TOKEN_HASH = addressToScriptHash("<USD_TOKEN_CONTRACT_ADDRESS_PLACEHOLDER>");
    // assuming USD token has 8 decimal: 1USD = 1_0000_0000
    // rate is 0.5 USD -> 1 CAT  <=>  0_5000_0000 -> 1_00
    // thus: rate is 50_0000
    private static final int EXCHANGE_RATE = 50_0000;

    @DisplayName("Transfer")
    private static Event3Args<Hash160, Hash160, Integer> onTransfer;
    @DisplayName("Mint")
    private static Event2Args<Hash160, Integer> onMint;
    @DisplayName("Destroy")
    private static Event2Args<Hash160, Integer> onDestroy;

    private static final int DECIMALS = 2;
    private static final ByteString ASSET_PREFIX = new ByteString("asset");
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

    public static boolean transfer(Hash160 from, Hash160 to, int amount, Object data) throws Exception {
        if (!Hash160.isValid(from) || !Hash160.isValid(to)) {
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
            Contract.call(to, "onNEP17Payment", CallFlags.All, new Object[]{from, amount, data});
        }

        return true;
    }

    public static int balanceOf(Hash160 account) throws Exception {
        if (!Hash160.isValid(account)) {
            throw new Exception("Argument is not a valid address.");
        }
        return getBalance(account);
    }

    @OnNEP17Payment
    public static void onPayment(Hash160 from, int usdAmount, Object data) throws Exception {
        if (USD_TOKEN_HASH != Runtime.getCallingScriptHash()) {
            throw new Exception("Invalid caller.");
        }
        if (!Hash160.isValid(from)) {
            throw new Exception("From address is not a valid address.");
        }
        if (usdAmount < 0) {
            throw new Exception("Invalid amount.");
        }

        if (from == OWNER && data instanceof Boolean && ((Boolean) data)) {
            // if come from owner and data is true, this is a donation, aka not exchange
            // just accept that and return
            // will be used when flamingo is online and dev team stake the USD
            return;
        }

        int catAmount = usdAmount / EXCHANGE_RATE;
        int usedAmount = catAmount * EXCHANGE_RATE;

        if (usedAmount != usdAmount) {
            // say if someone sent 1.000001USD, then he gets 2CAT,
            // but he will lose the small remainder
            // reject tx when this happens
            throw new Exception("Nonexchangeable amount detected.");
        }

        if (catAmount != 0) {
            addToBalance(from, catAmount);
            addToTotalSupply(catAmount);
        }
        onMint.fire(from, catAmount);
    }

    public static boolean destroyToken(Hash160 from, int catAmount) throws Exception {
        if (!Hash160.isValid(from)) {
            throw new Exception("From address is not a valid address.");
        }
        if (catAmount < 0) {
            throw new Exception("The destroy amount was negative.");
        }
        if (!Runtime.checkWitness(from) && from != Runtime.getCallingScriptHash()) {
            throw new Exception("Invalid sender signature. The sender of the tokens needs to be the signing account.");
        }

        int usdAmount = catAmount * EXCHANGE_RATE;

        if (getBalance(from) < catAmount) {
            throw new Exception("Insufficient amount.");
        }

        if (catAmount != 0) {
            deductFromBalance(from, catAmount);
            deductFromTotalSupply(catAmount);
        }
        onDestroy.fire(from, catAmount);
        if (usdAmount != 0) {
            // Might get false if we don't hold enough token
            // This might be useful when dev team want to destroy unsold token
            return (boolean) Contract.call(USD_TOKEN_HASH, "transfer", CallFlags.All,
                    new Object[]{Runtime.getExecutingScriptHash(), from, usdAmount, null});
        }
        return true;
    }


    @OnDeployment
    public static void deploy(Object data, boolean update) throws Exception {
        throwIfSignerIsNotOwner();
        if (!update) {
            if (getTotalSupply() != 0) {
                throw new Exception("Contract was already deployed.");
            }

            int initialSupply = 1_000_000_00;
            // Initialize supply
            Storage.put(sc, TOTAL_SUPPLY_KEY, initialSupply);
            // And allocate all tokens to the contract owner.
            Storage.put(sc, ASSET_PREFIX.concat(OWNER.toByteString()), initialSupply);
        }
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

    /**
     * Gets the address of the contract owner.
     *
     * @return the address of the contract owner.
     */
    public static Hash160 contractOwner() {
        return OWNER;
    }

    public static Hash160 usdTokenHash() {
        return USD_TOKEN_HASH;
    }

    public static int exchangeRate() {
        return EXCHANGE_RATE;
    }

    // -------------------- PRIVATE METHOD BELOW --------------------

    private static int getTotalSupply() {
        Integer i = Storage.getInteger(sc, TOTAL_SUPPLY_KEY);
        return i == null ? 0 : i;
    }

    private static void throwIfSignerIsNotOwner() throws Exception {
        if (!Runtime.checkWitness(OWNER)) {
            throw new Exception("The calling entity is not the owner of this contract.");
        }
    }

    private static void addToBalance(Hash160 key, int value) {
        assetMap.put(key.toByteString(), getBalance(key) + value);
    }

    private static void deductFromBalance(Hash160 key, int value) {
        int oldValue = getBalance(key);
        if (oldValue == value) {
            assetMap.delete(key.toByteString());
        } else {
            assetMap.put(key.toByteString(), oldValue - value);
        }
    }

    private static void addToTotalSupply(int value) {
        Storage.put(sc, TOTAL_SUPPLY_KEY, getTotalSupply() + value);
    }

    private static void deductFromTotalSupply(int value) {
        Storage.put(sc, TOTAL_SUPPLY_KEY, getTotalSupply() - value);
    }

    private static int getBalance(Hash160 key) {
        Integer i = assetMap.getInteger(key.toByteString());
        return i == null ? 0 : i;
    }
}
