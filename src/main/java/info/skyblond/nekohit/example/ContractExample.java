package info.skyblond.nekohit.example;

import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.neow3j.contract.SmartContract;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;

public class ContractExample {

    private static final Logger log = LoggerFactory.getLogger(ContractExample.class);

    public static void main(String[] args) throws Throwable {
        var contract = new SmartContract(new Hash160("0x2950f1d5992ed56539c7b1e4d8b9a4e449dc4dde"), Utils.NEOW3J);
        // test some unicode char here
        String[] descriptions = new String[] { "aaa", "bbb", "中文字符" };
        Long[] endTimestamps = new Long[descriptions.length];

        // create a WCA, shuold stake 5000_00
        log.info("Create WCA");
        String trueId = createWCA(contract, 1_00, 5000_00, descriptions, endTimestamps, "test_id");
        log.info("created WCA: {}", trueId);
        log.info("WCA info: {}", queryWCAJson(contract, trueId));
        log.info("owner cat balance: {}",
                Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash())));

        log.info("Pay stake");
        Utils.transferCatToken(Utils.CONTRACT_OWNER_WALLET, contract.getScriptHash(), 5000_00, trueId);
        log.info("WCA info: {}", queryWCAJson(contract, trueId));
        log.info("owner cat balance: {}",
                Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash())));

        log.info("Prepare test wallet");
        var testWallet = Utils.prepaWallet(5000_00);
        log.info("owner cat balance: {}",
                Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
        log.info("Test cat balance: {}",
                Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));

        // buy a WCA
        log.info("Buy WCA");
        Utils.transferCatToken(testWallet, contract.getScriptHash(), 5000_00, trueId);
        log.info("WCA info: {}", queryWCAJson(contract, trueId));
        log.info("owner cat balance: {}",
                Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
        log.info("Test cat balance: {}",
                Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));

        // finish WCA
        log.info("Finish WCA");
        for (int i = 0; i < descriptions.length; i++) {
            if (i == 1) // ignore second milestone
                continue;
            log.info("Finishing milestone {}", i);
            finishMilestone(contract, trueId, i, descriptions[i] + " Finished!");
            log.info("WCA info: {}", queryWCAJson(contract, trueId));
            log.info("owner cat balance: {}", Utils
                    .getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
            log.info("Test cat balance: {}", Utils
                    .getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));
        }
        // not sure why, but cannot exit by itself
        System.exit(0);
    }

    /**
     * Create WCA with Contract Owner account
     */
    private static String createWCA(SmartContract contract, int stakePer100Token, int totalAmount,
            String[] descriptions, Long[] endTimestamps, String identifier) throws Throwable {
        for (int i = 0; i < endTimestamps.length; i++) {
            endTimestamps[i] = System.currentTimeMillis() + 1800 * 1000 + i;
        }
        var tx = contract
                .invokeFunction("createWCA", ContractParameter.hash160(Utils.CONTRACT_OWNER_ACCOUNT),
                        ContractParameter.integer(stakePer100Token), ContractParameter.integer(totalAmount),
                        Utils.arrayParameter((Object[]) descriptions), Utils.arrayParameter((Object[]) endTimestamps),
                        ContractParameter.string(identifier))
                .signers(Signer.calledByEntry(Utils.CONTRACT_OWNER_ACCOUNT)).wallet(Utils.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();
        AtomicReference<String> trueId = new AtomicReference<>();
        if (response.getError() == null) {
            tx.track().blockingSubscribe(l -> {
                var exe = tx.getApplicationLog().getExecutions().get(0);
                trueId.set(exe.getStack().get(0).getString());
                log.info("createWCA tx: {}", tx.getTxId());
            });
        } else {
            log.error("Error when creating WCA: {}", response.getError().getMessage());
        }
        log.info("createWCA gas fee: {}", Utils.getGasWithDecimals(tx.getSystemFee() + tx.getNetworkFee()));
        return trueId.get();
    }

    private static void finishMilestone(SmartContract contract, String identifier, int index, String proofOfWork)
            throws Throwable {
        var tx = contract
                .invokeFunction("finishMilestone", ContractParameter.string(identifier),
                        ContractParameter.integer(index), ContractParameter.string(proofOfWork))
                .signers(Signer.calledByEntry(Utils.CONTRACT_OWNER_ACCOUNT)).wallet(Utils.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();
        if (response.getError() == null) {
            tx.track().blockingSubscribe(l -> {
                log.info("finishWCA tx: {}", tx.getTxId());
            });
        } else {
            log.error("Error when finishing WCA: {}", response.getError().getMessage());
        }
        log.info("finish WCA gas fee: {}", Utils.getGasWithDecimals(tx.getSystemFee() + tx.getNetworkFee()));
    }

    private static String queryWCAJson(SmartContract contract, String trueId) throws Throwable {
        var tx = contract.invokeFunction("queryWCA", ContractParameter.string(trueId))
                .signers(Signer.calledByEntry(Utils.CONTRACT_OWNER_ACCOUNT)).wallet(Utils.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();
        AtomicReference<String> result = new AtomicReference<>();
        if (response.getError() == null) {
            tx.track().blockingSubscribe(l -> {
                log.info("query WCA tx: {}", tx.getTxId());
                var exe = tx.getApplicationLog().getExecutions().get(0);
                result.set(exe.getStack().get(0).getString());
            });
        } else {
            log.error("Error when querying WCA: {}", response.getError().getMessage());
            return null;
        }
        log.info("query WCA gas fee: {}", Utils.getGasWithDecimals(tx.getSystemFee() + tx.getNetworkFee()));
        return result.get();
    }
}
