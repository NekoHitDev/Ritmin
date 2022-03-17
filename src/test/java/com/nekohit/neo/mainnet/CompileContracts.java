package com.nekohit.neo.mainnet;

import com.nekohit.neo.TestUtils;
import com.nekohit.neo.contract.NekoinToken;
import com.nekohit.neo.contract.WCAContract;
import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.ContractUtils;
import io.neow3j.utils.AddressUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompileContracts {
    private static final String CONTRACT_OWNER_ADDRESS = "NWWpkYtqeUwgHfbFMZurmKei6T85JtA1HQ";
    private static final List<Class<?>> CONTRACT_CLASS_LIST = List.of(WCAContract.class, NekoinToken.class);

    public static void main(String[] args) throws Exception {
        TestUtils.require(AddressUtils.isValidAddress(CONTRACT_OWNER_ADDRESS), "Invalid address");
        System.err.println("Your contract owner address is: " + CONTRACT_OWNER_ADDRESS);

        for (Class<?> clazz : CONTRACT_CLASS_LIST) {
            compileContract(clazz);
        }
    }

    private static <T> void compileContract(Class<T> clazz) throws IOException {
        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>", CONTRACT_OWNER_ADDRESS);

        // compile contract
        CompilationUnit compileResult = new Compiler().compile(clazz.getCanonicalName(), replaceMap);

        System.out.println("Contract compiled:");
        System.out.println(clazz.getCanonicalName());

        Path outputPath = Path.of("./compiled_contract");
        Files.createDirectories(outputPath);
        ContractUtils.writeNefFile(compileResult.getNefFile(),
                compileResult.getManifest().getName(), outputPath);
        ContractUtils.writeContractManifestFile(compileResult.getManifest(), outputPath);
    }
}
