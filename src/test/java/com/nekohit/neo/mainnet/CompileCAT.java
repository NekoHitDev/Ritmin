package com.nekohit.neo.mainnet;

import com.nekohit.neo.contract.CatToken;
import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.ContractUtils;
import io.neow3j.utils.AddressUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.nekohit.neo.contract.Utils.require;

public class CompileCAT {
    private static final String CONTRACT_OWNER_ADDRESS = "NWWpkYtqeUwgHfbFMZurmKei6T85JtA1HQ";

    public static void main(String[] args) throws Exception {
        require(AddressUtils.isValidAddress(CONTRACT_OWNER_ADDRESS), "Invalid address");
        System.err.println("Your contract owner address is: " + CONTRACT_OWNER_ADDRESS);

        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>", CONTRACT_OWNER_ADDRESS);

        // compile contract
        Class<CatToken> clazz = CatToken.class;
        CompilationUnit compileResult = new Compiler().compile(clazz.getCanonicalName(), replaceMap);

        System.out.println("Contract compiled:");
        System.out.println(clazz.getCanonicalName());

        Path outputPath = Path.of("./compiled_contract");
        Files.createDirectories(outputPath);
        ContractUtils.writeNefFile(compileResult.getNefFile(),
                compileResult.getManifest().getName(), outputPath);
        ContractUtils.writeContractManifestFile(compileResult.getManifest(), outputPath);
        System.exit(0);
    }
}
