package com.nekohit.neo;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.transaction.Transaction;
import io.neow3j.wallet.Account;

import java.math.BigInteger;
import java.util.Scanner;

public class TestUtils {
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

    public static Account readAccountWIF(Scanner scanner) {
        System.out.println("Paste account WIF:");
        String walletWIF = scanner.nextLine();
        // flush WIF out of screen
        for (int i = 0; i < 2000; i++) {
            System.out.println();
        }
        return Account.fromWIF(walletWIF);
    }


    /**
     * Similar to kotlin's require function. If condition is false, then exception is thrown
     *
     * @param condition the condition required to check
     * @param message   if condition is false, the message for exception
     */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }

    public interface MessageProvider {
        String provide();
    }

    public static void require(boolean condition, MessageProvider messageProvider) {
        if (!condition) {
            throw new RuntimeException(messageProvider.provide());
        }
    }

    /**
     * reverse the content in buffer in place, return the buffer.
     */
    public static byte[] reverse(byte[] buffer) {
        int size = buffer.length;
        byte temp;
        for (int i = 0; i < size / 2; i++) {
            temp = buffer[i];
            buffer[i] = buffer[size - i - 1];
            buffer[size - i - 1] = temp;
        }
        return buffer;
    }
}
