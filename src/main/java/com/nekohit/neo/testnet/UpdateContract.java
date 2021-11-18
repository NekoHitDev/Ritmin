package com.nekohit.neo.testnet;

import com.nekohit.neo.contract.CatToken;
import com.nekohit.neo.helper.Utils;
import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static com.nekohit.neo.helper.Utils.getGasFeeFromTx;

public class UpdateContract {
    private static final Neow3j NEOW3J = Neow3j.build(
            new HttpService("https://neo3-testnet.neoline.vip/")
    );

    // CatToken
    private static final Class<?> CONTRACT_CLASS = CatToken.class;
    private static final Hash160 CONTRACT_HASH = new Hash160("0xf461dff74f454e5016421341f115a2e789eadbd7");
    // WCA contract
//    private static final Class<?> CONTRACT_CLASS = WCAContract.class;
//    private static final Hash160 CONTRACT_HASH = new Hash160("0x199cd12a70bc554f7d3b0b91c5069546b15c0129");

    private static final SmartContract CONTRACT = new SmartContract(CONTRACT_HASH, NEOW3J);

    public static void main(String[] args) throws Throwable {
        System.out.println(CONTRACT_HASH.toAddress());
        Scanner scanner = new Scanner(System.in);
        Account deployAccount = Utils.readAccountWIF(scanner);

        // here we don't check the address, since only owner can update.
        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>", deployAccount.getAddress());
        // use fUSDT here
        Hash160 placeholder = new Hash160("0x83c442b5dc4ee0ed0e5249352fa7c75f65d6bfd6");
        replaceMap.put("<USD_TOKEN_CONTRACT_ADDRESS_PLACEHOLDER>", placeholder.toAddress());
        replaceMap.put("<USD_TOKEN_CONTRACT_HASH_PLACEHOLDER>", placeholder.toString());

        // compile contract
        CompilationUnit compileResult = new Compiler().compile(CONTRACT_CLASS.getCanonicalName(), replaceMap);
        System.out.println("Contract compiled:");
        System.out.println(CONTRACT_CLASS.getCanonicalName());

        System.out.println("Update following contract on public net:");
        System.out.println(CONTRACT_CLASS.getCanonicalName());
        System.out.println("Contract address: 0x" + CONTRACT_HASH);
        System.out.println("Contract owner account: " + deployAccount.getAddress());

        System.out.println("Type 'continue' to continue...");
        System.err.println("Note: Once confirmed, you CANNOT abort this process.");
        String line = scanner.nextLine();
        scanner.close();
        Utils.require(line.toLowerCase().trim().equals("continue"), "Canceled.");

        System.out.println("Updating contract... Do not stop this program!");

        byte[] manifestBytes = ObjectMapperFactory.getObjectMapper().writeValueAsBytes(compileResult.getManifest());
        Transaction tx = CONTRACT
                .invokeFunction(
                        "update",
                        ContractParameter.byteArray(compileResult.getNefFile().toArray()),
                        ContractParameter.byteArray(manifestBytes)
                )
                .signers(AccountSigner.calledByEntry(deployAccount))
                .sign();
        NeoSendRawTransaction response = tx.send();
        if (response.hasError()) {
            throw new Exception(String.format("Update failed: "
                    + "'%s'\n", response.getError().getMessage()));
        }
        System.out.println("Updated tx: 0x" + tx.getTxId());
        Await.waitUntilTransactionIsExecuted(tx.getTxId(), NEOW3J);
        System.out.println("Gas fee: " + getGasFeeFromTx(tx));

        System.out.println("Done.");
    }
}
