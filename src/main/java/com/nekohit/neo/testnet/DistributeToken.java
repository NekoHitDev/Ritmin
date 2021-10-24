package com.nekohit.neo.testnet;

import com.nekohit.neo.helper.Utils;
import io.neow3j.contract.FungibleToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Scanner;

public class DistributeToken {
    private static final Neow3j NEOW3J = Neow3j.build(
            new HttpService("https://neo3-testnet.neoline.vip:443"));

    private static final FungibleToken CAT_TOKEN = new FungibleToken(
            new Hash160("0xf461dff74f454e5016421341f115a2e789eadbd7"), NEOW3J);

    private static final BigInteger SATURATE_AMOUNT = BigInteger.valueOf(200_00L);

    public static void main(String[] args) throws Throwable {
        Scanner scanner = new Scanner(System.in);
        Account account = Utils.readAccountWIF(scanner);
        System.out.println("Account address: " + account.getAddress());
        System.out.println("Account balance: " + queryBalance(account.getScriptHash()));
        System.out.println("Paste address: ");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isEmpty()) {
                break;
            }
            Hash160 target = Hash160.fromAddress(line);
            transferToken(account, target);
        }

        scanner.close();
    }

    private static BigInteger queryBalance(Hash160 target) throws IOException {
        return CAT_TOKEN.getBalanceOf(target);
    }

    private static void transferToken(Account account, Hash160 to) throws Throwable {
        System.out.println("----------------------------------------");
        System.out.println("Transfer to: " + to);
        if (queryBalance(to).compareTo(SATURATE_AMOUNT) >= 0) {
            System.out.println("Address saturated, skip.");
            return;
        }
        Transaction tx = CAT_TOKEN.transfer(account, to, SATURATE_AMOUNT).sign();
        NeoSendRawTransaction resp = tx.send();
        if (resp.hasError()) {
            throw new Exception(String.format("Transfer failed: '%s'\n", resp.getError().getMessage()));
        } else {
            System.out.println("Done. Tx: 0x" + tx.getTxId());
        }
    }
}
