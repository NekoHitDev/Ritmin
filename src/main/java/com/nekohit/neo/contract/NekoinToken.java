package com.nekohit.neo.contract;

import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.*;
import io.neow3j.devpack.annotations.*;
import io.neow3j.devpack.constants.CallFlags;
import io.neow3j.devpack.constants.NeoStandard;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event3Args;

import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;

// TODO: Unit test for this contract
@SuppressWarnings("unused")
@ManifestExtra(key = "name", value = "Nekoin Token")
@ManifestExtra(key = "github", value = "https://github.com/NekoHitDev/Ritmin")
@ManifestExtra(key = "author", value = "NekoHitDev")
@ManifestExtra(key = "version", value = "v1.0.2")
// Contract as receiver
@Permission(contract = "*", methods = "onNEP17Payment")
// ContractManagement::update
@Permission(contract = "0xfffdc93764dbaddd97c48f252a53ea4643faa3fd", methods = {"update"})
@SupportedStandard(neoStandard = NeoStandard.NEP_17)
public class NekoinToken {
    private static final Hash160 OWNER = addressToScriptHash("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>");

    @DisplayName("Transfer")
    private static Event3Args<Hash160, Hash160, Integer> onTransfer;

    private static final int DECIMALS = 8;
    private static final String TOTAL_SUPPLY_KEY = "totalSupply";
    private static final String SYMBOL = "NEKOIN";
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

    @SuppressWarnings("DuplicatedCode")
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

    @Safe
    public static Hash160 contractOwner() {
        return OWNER;
    }


    // --------------------  MESSAGE READ&WRITE  --------------------

    private static final StorageMap messageMap = new StorageMap(sc, "message");

    @DisplayName("WriteMessage")
    private static Event1Arg<ByteString> onWriteMessage;

    public static ByteString writeMessage(ByteString content) {
        // hash is 32 bytes
        ByteString hash = CryptoLib.sha256(content);
        // throw error when hash exists
        assert messageMap.get(hash) == null : "Hash duplicated";
        // save to the map & fire the notify/event
        messageMap.put(hash, content);
        onWriteMessage.fire(hash);
        // return the hash
        return hash;
    }

    @Safe
    public static ByteString readMessage(ByteString hash) {
        // read content
        ByteString content = messageMap.get(hash);
        // throw error when hash not found
        assert content != null : "Hash not found";
        // return the content (NonNull)
        return content;
    }


    // --------------------      PRIVILEGED      --------------------

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

    public static void mint(int amount) {
        throwIfSignerIsNotOwner();
        assert amount > 0 : "The amount was non-positive.";
        addToBalance(OWNER, amount);
        addToTotalSupply(amount);
        onTransfer.fire(null, OWNER, amount);
    }

    public static void destroy(int amount) {
        throwIfSignerIsNotOwner();
        assert amount > 0 : "The amount was non-positive.";
        deductFromBalance(OWNER, amount);
        deductFromTotalSupply(amount);
        onTransfer.fire(OWNER, null, amount);
    }

    @OnVerification
    public static boolean verify() {
        throwIfSignerIsNotOwner();
        return true;
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
