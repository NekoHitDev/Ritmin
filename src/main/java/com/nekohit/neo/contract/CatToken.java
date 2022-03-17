package com.nekohit.neo.contract;

import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.*;
import io.neow3j.devpack.annotations.*;
import io.neow3j.devpack.constants.CallFlags;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.events.Event3Args;

import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;

@SuppressWarnings("unused")
@ManifestExtra(key = "name", value = "CAT Token")
@ManifestExtra(key = "github", value = "https://github.com/NekoHitDev/Ritmin")
@ManifestExtra(key = "author", value = "NekoHitDev")
@ManifestExtra(key = "version", value = "v1.0.2")
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
    // assuming USD token has 6 decimal: 1USD = 1_000000
    // rate is 0.5 USD -> 1 CAT  <=>  0_500000 -> 1_00
    // thus: rate is 5000
    private static final int EXCHANGE_RATE = 5000;

    @DisplayName("Transfer")
    private static Event3Args<Hash160, Hash160, Integer> onTransfer;

    private static final int DECIMALS = 2;
    private static final String TOTAL_SUPPLY_KEY = "totalSupply";
    private static final String SYMBOL = "CAT";
    private static final StorageContext sc = Storage.getStorageContext();
    private static final StorageMap assetMap = new StorageMap(sc, "asset");

    @Safe
    public static String symbol() {
        return SYMBOL;
    }

    @Safe
    public static int decimals() {
        return DECIMALS;
    }

    @Safe
    public static int totalSupply() {
        return getTotalSupply();
    }

    public static boolean transfer(Hash160 from, Hash160 to, int amount, Object data) {
        assert Hash160.isValid(from) && Hash160.isValid(to) : "From or To address is not a valid address.";
        assert amount >= 0 : "The transfer amount was negative.";
        assert Runtime.checkWitness(from) || from == Runtime.getCallingScriptHash()
                : "Invalid sender signature. The sender of the tokens needs to be the signing account.";

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

    @Safe
    public static int balanceOf(Hash160 account) {
        assert Hash160.isValid(account) : "Argument is not a valid address.";
        return getBalance(account);
    }

    @OnNEP17Payment
    public static void onPayment(Hash160 from, int usdAmount, Object data) {
        assert USD_TOKEN_HASH == Runtime.getCallingScriptHash() : "Invalid caller.";
        assert Hash160.isValid(from) : "From address is not a valid address.";
        assert usdAmount >= 0 : "Invalid amount.";

        if (from == OWNER && data instanceof Boolean && ((Boolean) data)) {
            // if come from owner and data is true, this is a donation, aka not exchange
            // just accept that and return
            // will be used when flamingo is online and dev team stake the USD
            return;
        }

        int catAmount = usdAmount / EXCHANGE_RATE;
        int usedAmount = catAmount * EXCHANGE_RATE;

        // say if someone sent 1.000001USD, then he gets 2CAT,
        // but he will lose the small remainder
        // reject tx when this happens
        assert usedAmount == usdAmount : "Nonexchangeable amount detected.";

        if (catAmount != 0) {
            addToBalance(from, catAmount);
            addToTotalSupply(catAmount);
        }
        onTransfer.fire(null, from, catAmount);
    }

    public static boolean destroyToken(Hash160 from, int catAmount) {
        assert Hash160.isValid(from) : "From address is not a valid address.";
        assert catAmount >= 0 : "The destroy amount was negative.";
        assert Runtime.checkWitness(from) || from == Runtime.getCallingScriptHash()
                : "Invalid sender signature. The sender of the tokens needs to be the signing account.";

        int usdAmount = catAmount * EXCHANGE_RATE;

        if (catAmount != 0) {
            deductFromBalance(from, catAmount);
            deductFromTotalSupply(catAmount);
        }
        onTransfer.fire(from, null, catAmount);
        if (usdAmount != 0) {
            // Might get false if we don't hold enough token
            // This might be useful when dev team want to destroy unsold token
            return (boolean) Contract.call(USD_TOKEN_HASH, "transfer", CallFlags.All,
                    new Object[]{Runtime.getExecutingScriptHash(), from, usdAmount, null});
        }
        return true;
    }

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (!update) {
            // Set initialize supply to zero
            Storage.put(sc, TOTAL_SUPPLY_KEY, 0);
        }
    }

    public static void update(ByteString script, String manifest) {
        throwIfSignerIsNotOwner();
        assert script.length() != 0 && manifest.length() != 0 : "The new contract script and manifest must not be empty.";
        ContractManagement.update(script, manifest);
    }

    @OnVerification
    public static boolean verify() {
        throwIfSignerIsNotOwner();
        return true;
    }

    /**
     * Gets the address of the contract owner.
     *
     * @return the address of the contract owner.
     */
    @Safe
    public static Hash160 contractOwner() {
        return OWNER;
    }

    @Safe
    public static Hash160 usdTokenHash() {
        return USD_TOKEN_HASH;
    }

    @Safe
    public static int exchangeRate() {
        return EXCHANGE_RATE;
    }

    // -------------------- PRIVATE METHOD BELOW --------------------

    private static int getTotalSupply() {
        return Storage.getIntOrZero(sc, TOTAL_SUPPLY_KEY);
    }

    private static void throwIfSignerIsNotOwner() {
        assert Runtime.checkWitness(OWNER) : "The calling entity is not the owner of this contract.";
    }

    private static void addToBalance(Hash160 key, int value) {
        assetMap.put(key.toByteString(), getBalance(key) + value);
    }

    private static void deductFromBalance(Hash160 key, int value) {
        int oldValue = getBalance(key);
        assert oldValue >= value : "Insufficient balance.";
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
        return assetMap.getIntOrZero(key.toByteString());
    }
}
