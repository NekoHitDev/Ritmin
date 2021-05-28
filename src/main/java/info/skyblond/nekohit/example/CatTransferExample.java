package info.skyblond.nekohit.example;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import io.neow3j.contract.FungibleToken;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Signer;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

public class CatTransferExample {
    public static void main(String[] args) throws Throwable {
        // use local neo-express
        var neow3j = Neow3j.build(new HttpService("http://127.0.0.1:50012"));
        // current Cat Token hash
        var token = new FungibleToken(new Hash160("0d3aeba74209d6460f2c5a83d75c70dbc153aaa2"), neow3j);
        // ContractOwner defind in `devnet.neo-express`
        var ownerAccount = new Account(ECKeyPair
                .create(new BigInteger("95ba67afd784f405e2800a0bcb875c035c41545d4a11e8995f6f1175d95c2952", 16)));
        var ownerWallet = Wallet.withAccounts(ownerAccount);
        // use genesis account here
        var testAccount = Account.fromAddress("NfMRbT87cZcBZ66Yrt2bHgFg65T93vFdBP");
        // query old balance
        var ownerOldBalance = token.getBalanceOf(ownerAccount.getScriptHash());
        var testOldBalance = token.getBalanceOf(testAccount.getScriptHash());
        System.out.println("Owner old balance: " + ownerOldBalance.toString(10));
        System.out.println("Test old balance: " + testOldBalance.toString(10));
        // do transfer
        token.transfer(ownerWallet, testAccount.getScriptHash(), BigInteger.valueOf(1000_00))
                .signers(Signer.calledByEntry(ownerAccount)).sign().send();
        // wait a new block
        TimeUnit.SECONDS.sleep(15);
        // query new balance
        var ownerNewBalance = token.getBalanceOf(ownerAccount.getScriptHash());
        var testNewBalance = token.getBalanceOf(testAccount.getScriptHash());
        System.out.println("Owner new balance: " + ownerNewBalance.toString(10));
        System.out.println("Test new balance: " + testNewBalance.toString(10));
        // look if balance changed correctly
    }
}
