package info.skyblond.nekohit.example;

import static info.skyblond.nekohit.example.Utils.CAT_TOKEN;
import static info.skyblond.nekohit.example.Utils.CONTRACT_OWNER_ACCOUNT;
import static info.skyblond.nekohit.example.Utils.CONTRACT_OWNER_WALLET;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import io.neow3j.transaction.Signer;
import io.neow3j.wallet.Account;

public class CatTransferExample {
    public static void main(String[] args) throws Throwable {
        // use genesis account here
        var testAccount = Account.fromAddress("NKvMswbG7QsRTEU9dkY2uY7ReKxmhXrc1M");
        // query old balance
        var ownerOldBalance = CAT_TOKEN.getBalanceOf(CONTRACT_OWNER_ACCOUNT.getScriptHash());
        var testOldBalance = CAT_TOKEN.getBalanceOf(testAccount.getScriptHash());
        System.out.println("Owner old balance: " + ownerOldBalance.toString(10));
        System.out.println("Test old balance: " + testOldBalance.toString(10));
        // do transfer
        CAT_TOKEN.transferFromDefaultAccount(CONTRACT_OWNER_WALLET, testAccount.getScriptHash(), BigInteger.valueOf(1000_00))
                .signers(Signer.calledByEntry(CONTRACT_OWNER_ACCOUNT)).sign().send();
        // wait a new block
        TimeUnit.SECONDS.sleep(15);
        // query new balance
        var ownerNewBalance = CAT_TOKEN.getBalanceOf(CONTRACT_OWNER_ACCOUNT.getScriptHash());
        var testNewBalance = CAT_TOKEN.getBalanceOf(testAccount.getScriptHash());
        System.out.println("Owner new balance: " + ownerNewBalance.toString(10));
        System.out.println("Test new balance: " + testNewBalance.toString(10));
        // look if balance changed correctly
    }
}
