package info.skyblond.nekohit.example;

import java.math.BigInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.neow3j.contract.SmartContract;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;

public class ContractExample {
    public static void main(String[] args) throws Throwable {
        var contract = new SmartContract(new Hash160("abaf7f243b12d37d468587d2bfe6755acae18c20"), Utils.NEOW3J);
        System.out.println(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash());
        var trueId = createWCA(contract, 1_00, 5000_00, "test_id");
        System.out.println(trueId);
        System.out.println(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash()).toString(10));
        Utils.CAT_TOKEN.transfer(Utils.CONTRACT_OWNER_WALLET, contract.getScriptHash(), BigInteger.valueOf(5000_00),
                ContractParameter.any(trueId)).sign().send();
        TimeUnit.SECONDS.sleep(15);
        var result = queryWCAJson(contract, trueId);
        System.out.println(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash()).toString(10));
        System.out.println(result);
    }

    /**
     * Create WCA with Contract Owner account
     */
    private static String createWCA(SmartContract contract, int stakePer100Token, int totalAmount, String identifier)
            throws Throwable {
        var tx = contract
                .invokeFunction("createWCA", ContractParameter.hash160(Utils.CONTRACT_OWNER_ACCOUNT),
                        ContractParameter.integer(stakePer100Token), ContractParameter.integer(totalAmount),
                        ContractParameter.integer(BigInteger.valueOf(System.currentTimeMillis() + 60 * 1000)),
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

    private static String queryWCAJson(SmartContract contract, String trueId) throws Throwable {
        var tx = contract
                .invokeFunction("queryWCA", ContractParameter.string(trueId))
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
}
