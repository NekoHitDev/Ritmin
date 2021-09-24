package com.nekohit.neo.testnet;

import com.nekohit.neo.helper.Utils;
import io.neow3j.contract.FungibleToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.script.ScriptBuilder;
import io.neow3j.transaction.Transaction;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Scanner;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static java.util.Arrays.asList;

public class DistributeToken {
    private static final Neow3j NEOW3J = Neow3j.build(
            new HttpService("https://testnet1.neo.coz.io:443"));

    private static final FungibleToken CAT_TOKEN = new FungibleToken(
            new Hash160("0xf461dff74f454e5016421341f115a2e789eadbd7"), NEOW3J);

    private static final BigInteger SATURATE_AMOUNT = BigInteger.valueOf(200_00L);

    private static final ScriptBuilder scriptBuilder = new ScriptBuilder();

    public static void main(String[] args) throws Throwable {
        Wallet wallet = Utils.readWalletWIF();
        System.out.println("Account address: " + wallet.getDefaultAccount().getAddress());
        System.out.println("Account balance: " + queryBalance(wallet.getDefaultAccount().getScriptHash()));
        Scanner scanner = new Scanner(System.in);
        System.out.println("Paste address: ");
        BigInteger acc = BigInteger.ZERO;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isEmpty()) break;
            Hash160 target = Hash160.fromAddress(line);

            acc = acc.add(transferToken(wallet.getDefaultAccount(), target));
        }

        System.out.println("Total: " + acc);

        Transaction tx = new TransactionBuilder(NEOW3J)
                .script(scriptBuilder.toArray())
//                .wallet(wallet)
                .signers(calledByEntry(wallet.getDefaultAccount()))
                .sign();

        NeoSendRawTransaction resp = tx.send();
        System.out.println("TxId: " + resp.getSendRawTransaction().getHash());
        Await.waitUntilTransactionIsExecuted(resp.getSendRawTransaction().getHash(), NEOW3J);
        for (NeoApplicationLog.Execution execution : tx.getApplicationLog().getExecutions()) {
            System.out.println(execution.getState());
            System.out.println(execution.getGasConsumed());
        }

        scanner.close();
    }

    private static BigInteger queryBalance(Hash160 target) throws IOException {
        return CAT_TOKEN.getBalanceOf(target);
    }

    private static BigInteger transferToken(Account account, Hash160 to) throws Throwable {
        System.out.println("----------------------------------------");
        System.out.println("Transfer to: " + to);
        BigInteger target = queryBalance(to);
        System.out.println("Target balance: " + target);
        if (target.compareTo(SATURATE_AMOUNT) >= 0) {
            System.out.println("Address saturated, skip.");
            return target;
        }

        target = SATURATE_AMOUNT.subtract(target);
        System.out.println("Amount: " + target);

        scriptBuilder
                .contractCall(
                        CAT_TOKEN.getScriptHash(),
                        "transfer",
                        asList(
                                ContractParameter.hash160(account.getScriptHash()),
                                ContractParameter.hash160(Hash160.fromAddress("NYukb9Nj59pQZ7SzubZeJUodrhczkXKD1Y")),
                                ContractParameter.integer(target),
                                null
                        )
                );
        return target;
    }
}
