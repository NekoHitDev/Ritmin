package info.skyblond.nekohit.neo;

import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;

public class ConvertHash160ToAddress {
    public static void main(String[] args) {
        var hash160 = new Hash160("0xf461dff74f454e5016421341f115a2e789eadbd7");
        System.out.println(hash160.toAddress());
        var account = Account.fromAddress(hash160.toAddress());
        System.out.println(account.getScriptHash());
    }
}
