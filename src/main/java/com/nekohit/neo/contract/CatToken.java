package com.nekohit.neo.contract;

import com.nekohit.neo.helper.StorageHelper;
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
@ManifestExtra(key = "version", value = "v1-RC1")
// Contract as receiver
@Permission(contract = "*", methods = "onNEP17Payment")
// ContractManagement::update
@Permission(contract = "0xfffdc93764dbaddd97c48f252a53ea4643faa3fd", methods = {"update"})
@SupportedStandards("NEP-17")
public class CatToken {
    private static final Hash160 OWNER = addressToScriptHash("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>");

    @DisplayName("Transfer")
    private static Event3Args<Hash160, Hash160, Integer> onTransfer;

    // issue #4, fixed supply with 1 billion
    private static final long INITIAL_SUPPLY = 1_000_000_000_00L;
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

    private static int getTotalSupply() {
        Integer i = Storage.getInteger(sc, TOTAL_SUPPLY_KEY);
        return i == null ? 0 : i;
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

    @OnDeployment
    public static void deploy(Object data, boolean update) throws Exception {
        throwIfSignerIsNotOwner();
        if (!update) {
            if(getTotalSupply() != 0) {
                throw new Exception("Contract was already deployed.");
            }
            // Initialize supply
            StorageHelper.put(sc, TOTAL_SUPPLY_KEY, INITIAL_SUPPLY);
            // And allocate all tokens to the contract owner.
            StorageHelper.put(sc, ASSET_PREFIX.concat(OWNER.toByteString()), INITIAL_SUPPLY);
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

    private static int getBalance(Hash160 key) {
        Integer i = assetMap.getInteger(key.toByteString());
        return i == null ? 0 : i;
    }
}
