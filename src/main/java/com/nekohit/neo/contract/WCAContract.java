package com.nekohit.neo.contract;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnVerification;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.contracts.ContractManagement;

import static com.nekohit.neo.contract.Utils.require;
import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;

@SuppressWarnings({"unused"})
@ManifestExtra(key = "name", value = "WCA Contract")
@ManifestExtra(key = "github", value = "https://github.com/NekoHitDev/Ritmin")
@ManifestExtra(key = "author", value = "NekoHitDev")
@ManifestExtra(key = "version", value = "emergency")
// ContractManagement::update
@Permission(contract = "0xfffdc93764dbaddd97c48f252a53ea4643faa3fd", methods = {"update"})
public class WCAContract {
    static final Hash160 OWNER = addressToScriptHash("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>");

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
}
