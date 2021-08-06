package info.skyblond.nekohit.neo;

import info.skyblond.nekohit.neo.contract.CatToken;
import info.skyblond.nekohit.neo.contract.WCAContract;
import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.ECKeyPair;
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
import io.neow3j.wallet.Wallet;

import java.math.BigInteger;
import java.util.Scanner;

public class UpdateContract {
    private static final Neow3j NEOW3J = Neow3j.build(
            new HttpService("https://testnet1.neo.coz.io")
    );

    private static final int CONFIRM_TIME = 30;
    private static final boolean REALLY_DEPLOY_FLAG = false;
    private static final Class<?> CONTRACT_CLASS = WCAContract.class;
    private static final Hash160 CONTRACT_HASH = new Hash160("0x7eaf33edde0cb499e84d940df65d875bed10b612");
    private static final SmartContract CONTRACT = new SmartContract(CONTRACT_HASH, NEOW3J);

    public static void main(String[] args) throws Throwable {
        // compile contract
        CompilationUnit compileResult = new Compiler().compile(CONTRACT_CLASS.getCanonicalName());
        System.out.println("Contract compiled:");
        System.out.println(CONTRACT_CLASS.getCanonicalName());

        Scanner scanner = new Scanner(System.in);
        System.out.println("Paste contract owner account WIF:");
        String walletWIF = scanner.nextLine();
        // flush WIF out of screen
        for (int i = 0; i < 1000; i++) {
            System.out.println();
        }
        Wallet deployWallet = Wallet.withAccounts(Account.fromWIF(walletWIF));

        System.out.println("Update following contract on public net:");
        System.out.println(CONTRACT_CLASS.getCanonicalName());
        System.out.println("Contract address: 0x" + CONTRACT_HASH);
        System.out.println("Contract owner account: " + deployWallet.getDefaultAccount().getAddress());

        System.out.println("Type 'continue' to continue...");
        String line = scanner.nextLine();
        scanner.close();
        if (!line.toLowerCase().trim().equals("continue")) {
            System.out.println("Canceled.");
            return;
        }

        System.out.println("This is the last chance to stop the process.");
        for (int i = CONFIRM_TIME; i > 0; i--) {
            if (i % 10 == 0 || i <= 5) {
                System.out.println("In " + i + " second(s)...");
            }
            Thread.sleep(1000);
        }
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
                    .wallet(deployWallet)
                    .sign();
            NeoSendRawTransaction response = tx.send();
            if (response.hasError()) {
                throw new Exception(String.format("Update was not successful. Error message from neo-node was: "
                        + "'%s'\n", response.getError().getMessage()));
            }
            System.out.println("Updated tx: 0x" + tx.getTxId());
            Await.waitUntilTransactionIsExecuted(tx.getTxId(), NEOW3J);
            System.out.println("Gas fee: " + getGasFeeFromTx(tx));
        } else {
            System.out.println("This is a simulation. No contract is deployed.");
        }

        System.out.println("Done.");
    }

    private static double getGasFeeFromTx(Transaction tx) {
        long fraction = tx.getSystemFee() + tx.getNetworkFee();
        return fraction / Math.pow(10, 8);
    }
}
