package info.skyblond.nekohit.example;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.neow3j.contract.SmartContract;
import io.neow3j.devpack.Contract;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;

public class ContractExample {

    private static final Logger log = LoggerFactory.getLogger(ContractExample.class);
    public static void main(String[] args) throws Throwable {
        /* var contract = new SmartContract(new Hash160("0x17e200f0c7f9acea91f4d8f9d53adf2f900bd738"), Utils.NEOW3J); */

        // create a WCA
        String trueId = createWCA2(1_00, 5000_00, "test_id");
        log.info("created WCA: {}", trueId);

        String balanceStart = Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash()).toString(10);
        log.info("owner cat balance: {}", Utils.getCatWithDecimals(Long.valueOf(balanceStart)));

        Utils.CAT_TOKEN.transfer(Utils.CONTRACT_OWNER_WALLET, Utils.WCA_CONTRACT_HASH, BigInteger.valueOf(5000_00),
            ContractParameter.string(trueId)).signers(Signer.global(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash()))
            .sign()
            .send();

        // Sign by global is required to let WCA contract use Token's signature
        /* Utils.CAT_TOKEN.transfer(Utils.CONTRACT_OWNER_WALLET, Utils.WCA_CONTRACT_HASH, BigInteger.valueOf(5000_00),
            ContractParameter.string(trueId)).signers(Signer.global(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash()))
            .sign()
            .send();
        TimeUnit.SECONDS.sleep(5); */

        /* var result = queryWCAJson(contract, trueId);
        System.out.println(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash()).toString(10));
        System.out.println(result);

        var testWallet = Wallet.create();
        // prepare a test account
        Utils.CAT_TOKEN.transfer(Utils.CONTRACT_OWNER_WALLET, testWallet.getDefaultAccount().getScriptHash(),
                BigInteger.valueOf(5000_00)).sign().send();
        TimeUnit.SECONDS.sleep(15);
        System.out.println(Utils.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash()).toString(10));
        System.out.println(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash()).toString(10));  */

        // buy a WCA
       /*  var tx = Utils.CAT_TOKEN
                .invokeFunction("transfer", ContractParameter.hash160(testWallet.getDefaultAccount().getScriptHash()),
                        ContractParameter.hash160(contract.getScriptHash()),
                        ContractParameter.integer(BigInteger.valueOf(5000_00)), ContractParameter.string(trueId))
                .signers(Signer.global(testWallet.getDefaultAccount().getScriptHash())).wallet(testWallet).sign();
        tx.send();
        tx.track().subscribe(l -> {
            System.out.println(tx.getTxId().toString());
            System.out.println(tx.getApplicationLog().getTransactionId().toString());
        });
        TimeUnit.SECONDS.sleep(20);
        // query result
        result = queryWCAJson(contract, trueId);
        System.out.println(Utils.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash()).toString(10));
        System.out.println(result);

        // finish WCA
        finishWCA(contract, trueId, true);
        // query result
        result = queryWCAJson(contract, trueId);
        System.out.println(Utils.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash()).toString(10));
        System.out.println(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash()).toString(10));
        System.out.println(result); */
    }

    /**
     * Create WCA with Contract Owner account
     */
    private static String createWCA(SmartContract contract, int stakePer100Token, int totalAmount, String identifier)
            throws Throwable {
        var tx = contract
                .invokeFunction("createWCA", ContractParameter.hash160(Utils.CONTRACT_OWNER_ACCOUNT),
                        ContractParameter.integer(stakePer100Token), ContractParameter.integer(totalAmount),
                        ContractParameter.integer(BigInteger.valueOf(System.currentTimeMillis() + 180 * 1000)),
                        ContractParameter.string(identifier))
                .signers(Signer.calledByEntry(Utils.CONTRACT_OWNER_ACCOUNT)).wallet(Utils.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();
        AtomicReference<String> trueId = new AtomicReference<>();
        if (response.getError() == null) {
            var countDownLatch = new CountDownLatch(1);
            tx.track().subscribe(l -> {
                var exe = tx.getApplicationLog().getExecutions().get(0);
                trueId.set(exe.getStack().get(0).getString());
                countDownLatch.countDown();
            });
            countDownLatch.await();
        } else {
            return null;
        }
        return trueId.get();
    }

    private static void finishWCA(SmartContract contract, String identifier, boolean finished) throws Throwable {
        var tx = contract
                .invokeFunction("finishWCA", ContractParameter.string(identifier), ContractParameter.bool(finished))
                .signers(Signer.calledByEntry(Utils.CONTRACT_OWNER_ACCOUNT)).wallet(Utils.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();
        if (response.getError() == null) {
            var countDownLatch = new CountDownLatch(1);
            tx.track().subscribe(l -> {
                countDownLatch.countDown();
            });
            countDownLatch.await();
        }
    }

    private static String queryWCAJson(SmartContract contract, String trueId) throws Throwable {
        var tx = contract.invokeFunction("queryWCA", ContractParameter.string(trueId))
                .signers(Signer.calledByEntry(Utils.CONTRACT_OWNER_ACCOUNT)).wallet(Utils.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();
        AtomicReference<String> result = new AtomicReference<>();
        if (response.getError() == null) {
            var countDownLatch = new CountDownLatch(1);
            tx.track().subscribe(l -> {
                var exe = tx.getApplicationLog().getExecutions().get(0);
                result.set(exe.getStack().get(0).getString());
                countDownLatch.countDown();
            });
            countDownLatch.await();
        } else {
            return null;
        }
        return result.get();
    }

    private static String createWCA2(int stakePer100Token, int totalAmount, String identifier) {
        List<ContractParameter> params = new ArrayList<>();
        params.add(ContractParameter.hash160(Utils.CONTRACT_OWNER_ACCOUNT));
        params.add(ContractParameter.integer(stakePer100Token));
        params.add(ContractParameter.integer(totalAmount));
        params.add(ContractParameter.integer(BigInteger.valueOf(System.currentTimeMillis() + 180 * 1000)));
        params.add(ContractParameter.string(identifier));
        InvocationResult result = null;
        try {
            result = Utils.NEOW3J.invokeFunction(Hash160.fromAddress("NR6We6gPCDmrYzv8PsVGvRzHG9ki5TpZrd"), 
            "createWCA", params, Signer.calledByEntry(Utils.CONTRACT_OWNER_ACCOUNT))
            .send()
            .getResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        log.info("gas cost of createWCA: {}", Utils.getGasWithDecimals(Integer.valueOf(result.getGasConsumed())));
        return result.getStack().stream().findFirst().get().getString();
    }
}
