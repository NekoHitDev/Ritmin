package com.nekohit.neo.mainnet;

import com.nekohit.neo.TestUtils;
import com.nekohit.neo.contract.CatToken;
import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.ContractUtils;
import io.neow3j.contract.FungibleToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.Hash160;
import io.neow3j.utils.AddressUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CompileCAT {
    private static final String CONTRACT_OWNER_ADDRESS = "NWWpkYtqeUwgHfbFMZurmKei6T85JtA1HQ";

    // https://neo3.neotube.io/tokens/nep17/0xcd48b160c1bbc9d74997b803b9a7ad50a4bef020
    // Flamingo's fUSDT
    private static final Hash160 USD_CONTRACT_SCRIPT_HASH = new Hash160("0xcd48b160c1bbc9d74997b803b9a7ad50a4bef020");

    public static void main(String[] args) throws Exception {
        TestUtils.require(AddressUtils.isValidAddress(CONTRACT_OWNER_ADDRESS), "Invalid address");
        System.err.println("Your contract owner address is: " + CONTRACT_OWNER_ADDRESS);
        System.err.println("Your usd token script has is: 0x" + USD_CONTRACT_SCRIPT_HASH);

        FungibleToken fUSDT = new FungibleToken(USD_CONTRACT_SCRIPT_HASH, Neow3j.build(
                new HttpService("https://n3seed1.ngd.network:10332/")));
        String usdTokenSymbol = fUSDT.getSymbol();
        System.err.println("USD token symbol: " + usdTokenSymbol);
        TestUtils.require(usdTokenSymbol.equals("fUSDT"), "Invalid USD token");

        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>", CONTRACT_OWNER_ADDRESS);
        replaceMap.put("<USD_TOKEN_CONTRACT_ADDRESS_PLACEHOLDER>", USD_CONTRACT_SCRIPT_HASH.toAddress());
        replaceMap.put("<USD_TOKEN_CONTRACT_HASH_PLACEHOLDER>", USD_CONTRACT_SCRIPT_HASH.toString());

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
