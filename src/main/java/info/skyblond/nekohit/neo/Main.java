package info.skyblond.nekohit.neo;

import java.util.Scanner;

import org.apache.commons.codec.binary.Hex;
import info.skyblond.nekohit.neo.contract.CatToken;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.Transaction;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

public class Main {
    private static final Neow3j NEOW3J = Neow3j.build(
        new HttpService("http://seed1t.neo.org:20332")
    );

    private static final int CONFIRM_TIME = 30;
    private static final boolean REALLY_DEPLOY_FLAG = false;
    
    public static void main(String[] args) throws Throwable {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Paste deploy account WIF:");
        var walletWIF = scanner.nextLine();
        // flush WIF out of screen
        for (int i = 0; i < 1000; i++) {
            System.out.println();
        }
        scanner.close();
        var deployWallet = Wallet.withAccounts(Account.fromWIF(walletWIF));
        System.out.println("Using account: " + deployWallet.getDefaultAccount().getAddress());
        System.out.println("Terminate this program to stop the process.");
        for (int i = CONFIRM_TIME; i > 0; i--) {
            if (i % 10 == 0 || i <= 5) System.out.println("In " + i + " second(s)...");
            Thread.sleep(1000);
        }

        var contractClass = CatToken.class;
        System.out.println("Deploy follow contract on public net:");
        System.out.println(contractClass.getCanonicalName());
        System.out.println("Using account: " + deployWallet.getDefaultAccount().getAddress());
        System.out.println("Terminate this program to stop the process.");
        for (int i = CONFIRM_TIME; i > 0; i--) {
            if (i % 10 == 0 || i <= 5) System.out.println("In " + i + " second(s)...");
            Thread.sleep(1000);
        }
        System.out.println("Compiling contract...");

        var compileResult = new Compiler().compile(contractClass.getCanonicalName());
        var contractHash = SmartContract.calcContractHash(
            deployWallet.getDefaultAccount().getScriptHash(), 
            compileResult.getNefFile().getCheckSumAsInteger(), 
            compileResult.getManifest().getName()
        );

        System.out.println("Deploy following contract on public net:");
        System.out.println(contractClass.getCanonicalName());
        System.out.println("Will deployed to 0x" + contractHash);
        System.out.println("Using account: " + deployWallet.getDefaultAccount().getAddress());

        System.out.println("This is the last chance to stop the process.");
        for (int i = CONFIRM_TIME; i > 0; i--) {
            if (i % 10 == 0 || i <= 5) System.out.println("In " + i + " second(s)...");
            Thread.sleep(1000);
        }
        System.out.println("Deploying contract... Do not stop this program!");

        if (REALLY_DEPLOY_FLAG) {
            var tx = new ContractManagement(NEOW3J)
            .deploy(compileResult.getNefFile(), compileResult.getManifest())
            .signers(Signer.global(deployWallet.getDefaultAccount().getScriptHash()))
            .wallet(deployWallet)
            .sign();
            var response = tx.send();
            if (response.hasError()) {
                throw new Exception(String.format("Deployment was not successful. Error message from neo-node was: "
                        + "'%s'\n", response.getError().getMessage()));
            } 
            Await.waitUntilTransactionIsExecuted(tx.getTxId(), NEOW3J);
            System.out.println("Gas fee: " + getGasFeeFromTx(tx));
        } else {
            System.out.println("This is a simulation. No contract is deployed.");
        }

        System.out.println("Using account: " + deployWallet.getDefaultAccount().getAddress());
        System.out.println("Contract: " + contractClass.getCanonicalName());
        System.out.println("Deployed hash: 0x" + contractHash);
        System.out.println("Little endian array: " + Hex.encodeHexString(contractHash.toLittleEndianArray(), true));
    }

    private static double getGasFeeFromTx(Transaction tx) {
        var fraction = tx.getSystemFee() + tx.getNetworkFee();
        return fraction / Math.pow(10, 8);
    }
}
