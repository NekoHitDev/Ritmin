package com.nekohit.neo.contract;

import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.*;
import io.neow3j.devpack.annotations.*;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.events.Event3Args;

import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;

@SuppressWarnings("unused")
@ManifestExtra(key = "name", value = "CAT Token")
@ManifestExtra(key = "github", value = "https://github.com/NekoHitDev/Ritmin")
@ManifestExtra(key = "author", value = "NekoHitDev")
@ManifestExtra(key = "version", value = "emergency")
// ContractManagement::update
@Permission(contract = "0xfffdc93764dbaddd97c48f252a53ea4643faa3fd", methods = {"update"})
@SupportedStandards("NEP-17")
public class CatToken {
    private static final Hash160 OWNER = addressToScriptHash("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>");

    @DisplayName("Transfer")
    private static Event3Args<Hash160, Hash160, Integer> onTransfer;

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

    public static boolean transfer(Hash160 from, Hash160 to, int amount, Object data) {
        Utils.abort();
        return false;
    }

    public static int balanceOf(Hash160 account) throws Exception {
        if (!Hash160.isValid(account)) {
            throw new Exception("Argument is not a valid address.");
        }
        return getBalance(account);
    }

    @OnDeployment
    public static void deploy(Object data, boolean update) {
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

    private static int getBalance(Hash160 key) {
        Integer i = assetMap.getInteger(key.toByteString());
        return i == null ? 0 : i;
    }
}
