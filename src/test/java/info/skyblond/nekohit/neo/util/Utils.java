package info.skyblond.nekohit.neo.util;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

public class Utils {
    public static Account createAccountFromPrivateKey(String privateKey) {
        return new Account(ECKeyPair.create(
                new BigInteger(privateKey, 16)
        ));
    }
}
