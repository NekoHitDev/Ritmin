package info.skyblond.nekohit.neo.helper;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.transaction.Transaction;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

import java.math.BigInteger;
import java.util.Scanner;

public class Utils {
    /**
     * Similar to kotlin's require function. If condition is false, then exception is thrown
     *
     * @param condition the condition required to check
     * @param message   if condition is false, the message for exception
     * @throws Exception if the condition is failed
     */
    public static void require(boolean condition, String message) throws Exception {
        if (!condition) {
            throw new Exception(message);
        }
    }

    public static Account createAccountFromPrivateKey(String privateKey) {
        return new Account(ECKeyPair.create(
                new BigInteger(privateKey, 16)
        ));
    }

    /**
     * Get gas fee from given test
     *
     * @param tx the transaction
     * @return the gas fee in decimal representation
     */
    public static double getGasFeeFromTx(Transaction tx) {
        long fraction = tx.getSystemFee() + tx.getNetworkFee();
        return fraction / Math.pow(10, 8);
    }

    public static Wallet readWalletWIF() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Paste account WIF:");
        String walletWIF = scanner.nextLine();
        // flush WIF out of screen
        for (int i = 0; i < 2000; i++) {
            System.out.println();
        }
        return Wallet.withAccounts(Account.fromWIF(walletWIF));
    }
}
