package info.skyblond.nekohit.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import io.neow3j.types.Hash160;
import io.neow3j.contract.NefFile;
import io.neow3j.contract.SmartContract;
import io.neow3j.serialization.exceptions.DeserializationException;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;

public class GetScriptHashFromContractFile {

    public static void main(String[] args) throws DeserializationException, IOException {

        // NEF:
        File contractNefFile = Paths.get("build", "neow3j", "WCAContract.nef").toFile();
        NefFile nefFile = NefFile.readFromFile(contractNefFile);
        // Manifest file:
        File contractManifestFile = Paths.get("build", "neow3j", "WCAContract.manifest.json").toFile();
        ContractManifest manifest;
        try (FileInputStream s = new FileInputStream(contractManifestFile)) {
            manifest = ObjectMapperFactory.getObjectMapper().readValue(s, ContractManifest.class);
        }

        // Get and print the contract hash
        Hash160 contractHash = SmartContract.getContractHash(Utils.GENESIS_ACCOUNT.getScriptHash(),
                nefFile.getCheckSumAsInteger(), manifest.getName());
        System.out.println("Contract Hash: " + contractHash);
        System.out.println("Contract Address: " + contractHash.toAddress());
    }

}
