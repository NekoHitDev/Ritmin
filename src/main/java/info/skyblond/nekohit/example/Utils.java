package info.skyblond.nekohit.example;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import io.neow3j.contract.FungibleToken;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

public class Utils {
    public static final Neow3j NEOW3J = Neow3j.build(new HttpService("http://127.0.0.1:50012"));

    public static final FungibleToken CAT_TOKEN = new FungibleToken(
            new Hash160("df5526bbbaa3a4f01d14d4455f564c45859f2fa7"), NEOW3J);

    // ContractOwner defind in `devnet.neo-express`
    public static final Account CONTRACT_OWNER_ACCOUNT = new Account(
            ECKeyPair.create(new BigInteger("95ba67afd784f405e2800a0bcb875c035c41545d4a11e8995f6f1175d95c2952", 16)));

    public static final Wallet CONTRACT_OWNER_WALLET = Wallet.withAccounts(CONTRACT_OWNER_ACCOUNT);

    public static final Account GENESIS = Account.fromAddress("NKvMswbG7QsRTEU9dkY2uY7ReKxmhXrc1M");

    public static final Hash160 WCA_CONTRACT_HASH = Hash160.fromAddress("NR6We6gPCDmrYzv8PsVGvRzHG9ki5TpZrd");

    public static void transferCatToken(Wallet wallet, Hash160 from, Hash160 to, long amount, Object data)
            throws IOException, Throwable {
        CAT_TOKEN
                .transferFromSpecificAccounts(wallet, to, BigInteger.valueOf(amount), ContractParameter.any(data), from)
                .signers(Signer.calledByEntry(from)).wallet(wallet).sign().send();
        TimeUnit.SECONDS.sleep(15);
    }

    public static double getGasWithDecimals(int value) {
        return value / Math.pow(10, 8);
    }

    public static double getCatWithDecimals(long value) {
        return value / Math.pow(10, 2);
    }
}
