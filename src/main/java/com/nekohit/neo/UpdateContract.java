package com.nekohit.neo;

import com.nekohit.neo.contract.WCAContract;
import com.nekohit.neo.helper.Utils;
import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.FungibleToken;
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
import io.neow3j.wallet.Wallet;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static com.nekohit.neo.helper.Utils.getGasFeeFromTx;

public class UpdateContract {
    private static final Neow3j NEOW3J = Neow3j.build(
            new HttpService("https://testnet1.neo.coz.io")
    );

    private static final boolean REALLY_DEPLOY_FLAG = false;
    private static final Class<?> CONTRACT_CLASS = WCAContract.class;
    private static final Hash160 CONTRACT_HASH = new Hash160("0x7eaf33edde0cb499e84d940df65d875bed10b612");
    private static final SmartContract CONTRACT = new SmartContract(CONTRACT_HASH, NEOW3J);

    public static void main(String[] args) throws Throwable {
        Wallet deployWallet = Utils.readWalletWIF();
        Scanner scanner = new Scanner(System.in);

        // here we don't check the address, since only owner can update.
        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("<CONTRACT_OWNER_ADDRESS_PLACEHOLDER>", deployWallet.getDefaultAccount().getAddress());
        if (CONTRACT_CLASS == WCAContract.class) {
            System.out.println("Paste CatToken address in hash160 (0x...): ");
            String catHash = scanner.nextLine();
            FungibleToken cat = new FungibleToken(new Hash160(catHash), NEOW3J);
            Utils.require("CAT".equals(cat.getSymbol()), "Token symbol not match!");
            Utils.require("CatToken".equals(cat.getName()), "Token name not match!");
            replaceMap.put("<CAT_TOKEN_CONTRACT_ADDRESS_PLACEHOLDER>", cat.getScriptHash().toAddress());
            replaceMap.put("<CAT_TOKEN_CONTRACT_HASH_PLACEHOLDER>", cat.getScriptHash().toString());
            System.out.println("Validate CatToken contract address: " + cat.getScriptHash().toAddress());
        }

        // compile contract
        CompilationUnit compileResult = new Compiler().compile(CONTRACT_CLASS.getCanonicalName(), replaceMap);
        System.out.println("Contract compiled:");
        System.out.println(CONTRACT_CLASS.getCanonicalName());

        System.out.println("Update following contract on public net:");
        System.out.println(CONTRACT_CLASS.getCanonicalName());
        System.out.println("Contract address: 0x" + CONTRACT_HASH);
        System.out.println("Contract owner account: " + deployWallet.getDefaultAccount().getAddress());

        System.out.println("Type 'continue' to continue...");
        System.err.println("Note: Once confirmed, you CANNOT abort this process.");
        String line = scanner.nextLine();
        scanner.close();
        Utils.require(line.toLowerCase().trim().equals("continue"), "Canceled.");

        System.out.println("Updating contract... Do not stop this program!");

        if (REALLY_DEPLOY_FLAG) {
            byte[] manifestBytes = ObjectMapperFactory.getObjectMapper().writeValueAsBytes(compileResult.getManifest());
            Transaction tx = CONTRACT
                    .invokeFunction(
                            "update",
                            ContractParameter.byteArray(compileResult.getNefFile().toArray()),
                            ContractParameter.byteArray(manifestBytes)
                    )
                    .signers(AccountSigner.calledByEntry(deployWallet.getDefaultAccount()))
//                    .wallet(deployWallet)
                    .sign();
            NeoSendRawTransaction response = tx.send();
            if (response.hasError()) {
                throw new Exception(String.format("Update failed: "
                        + "'%s'\n", response.getError().getMessage()));
            }
            System.out.println("Updated tx: 0x" + tx.getTxId());
            Await.waitUntilTransactionIsExecuted(tx.getTxId(), NEOW3J);
            System.out.println("Gas fee: " + getGasFeeFromTx(tx));
        } else {
            System.err.println("This is a simulation. No contract is deployed.");
        }

        System.out.println("Done.");
    }
}
