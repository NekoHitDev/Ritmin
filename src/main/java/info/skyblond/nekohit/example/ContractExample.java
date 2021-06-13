package info.skyblond.nekohit.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.neow3j.contract.SmartContract;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.utils.Await;

import java.util.Arrays;

public class ContractExample {

    private static final Logger log = LoggerFactory.getLogger(ContractExample.class);

    public static void main(String[] args) throws Throwable {
        var contract = Constants.WCA_CONTRACT;
        // test some unicode char here
        String[] descriptions = new String[] { "ms0", "ms1", "ms2", "ms3" };
        Long[] endTimestamps = new Long[descriptions.length];
        int thresholdIndex = 1;

        // create a WCA, should stake 5000_00
        log.info("Create WCA");
        String trueId = createWCA(contract, 1_00, 5000_00, descriptions, endTimestamps, thresholdIndex, "test_id" + System.currentTimeMillis());
        log.info("created WCA: {}", trueId);
        log.info("WCA info: {}", queryWCAJson(contract, trueId));
        log.info("owner cat balance: {}",
                Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(Constants.CONTRACT_OWNER_ACCOUNT.getScriptHash())));

        log.info("Pay stake");
        Utils.transferCatToken(Constants.CONTRACT_OWNER_WALLET, contract.getScriptHash(), 5000_00, trueId);
        log.info("WCA info: {}", queryWCAJson(contract, trueId));
        log.info("owner cat balance: {}",
                Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(Constants.CONTRACT_OWNER_ACCOUNT.getScriptHash())));

        log.info("Prepare test wallet");
        var testWallet = Utils.prepaTestWallet(5000_00);
        log.info("owner cat balance: {}",
                Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(Constants.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
        log.info("Test cat balance: {}",
                Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));

        // buy a WCA
        log.info("Buy WCA 1/2");
        Utils.transferCatToken(testWallet, contract.getScriptHash(), 3000_00, trueId);
        log.info("WCA info: {}", queryWCAJson(contract, trueId));
        log.info("owner cat balance: {}",
                Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(Constants.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
        log.info("Test cat balance: {}",
                Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));
        log.info("Buy WCA 2/2");
        Utils.transferCatToken(testWallet, contract.getScriptHash(), 2000_00, trueId);
        log.info("WCA info: {}", queryWCAJson(contract, trueId));
        log.info("owner cat balance: {}",
                Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(Constants.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
        log.info("Test cat balance: {}",
                Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));
        
        // finish WCA
        log.info("Finish WCA");
        for (int i = 0; i < descriptions.length; i++) {
            if (i == 2) // ignore ms2
                continue;
            log.info("Finishing milestone {}", i);
            finishMilestone(contract, trueId, i, descriptions[i] + " Finished!");
            log.info("WCA info: {}", queryWCAJson(contract, trueId));
            log.info("owner cat balance: {}", Utils
                    .getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(Constants.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
            log.info("Test cat balance: {}", Utils
                    .getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));
        }
        // not sure why, but cannot exit by itself
        System.exit(0);
    }

    /**
     * Create WCA with Contract Owner account
     */
    private static String createWCA(SmartContract contract, int stakePer100Token, int totalAmount,
            String[] descriptions, Long[] endTimestamps, int thresholdIndex, String identifier
        ) throws Throwable {
        for (int i = 0; i < endTimestamps.length; i++) {
            endTimestamps[i] = System.currentTimeMillis() + 1800 * 1000 + i;
        }
        var tx = contract
                .invokeFunction(
                        "createWCA", 
                        ContractParameter.hash160(Constants.CONTRACT_OWNER_ACCOUNT),
                        ContractParameter.integer(stakePer100Token), 
                        ContractParameter.integer(totalAmount),
                        ContractParameter.array(Arrays.asList(descriptions)), 
                        ContractParameter.array(Arrays.asList(endTimestamps)),
                        ContractParameter.integer(thresholdIndex),
                        ContractParameter.string(identifier))
                .signers(Signer.calledByEntry(Constants.CONTRACT_OWNER_ACCOUNT)).wallet(Constants.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();
        if (response.hasError()) {
            throw new Exception(response.getError().getMessage());
        }

        log.info("createWCA tx: {}", tx.getTxId());
        Await.waitUntilTransactionIsExecuted(tx.getTxId(), Constants.NEOW3J);
        
        log.info("createWCA gas fee: {}", Utils.getGasWithDecimals(tx.getSystemFee() + tx.getNetworkFee()));
        
        return tx.getApplicationLog().getExecutions().get(0).getStack().get(0).getString();
    }

    private static void finishMilestone(SmartContract contract, String identifier, int index, String proofOfWork)
            throws Throwable {
        var tx = contract
                .invokeFunction("finishMilestone", ContractParameter.string(identifier),
                        ContractParameter.integer(index), ContractParameter.string(proofOfWork))
                .signers(Signer.calledByEntry(Constants.CONTRACT_OWNER_ACCOUNT)).wallet(Constants.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();

        if (response.hasError()) {
            throw new Exception(response.getError().getMessage());
        }

        log.info("finishMilestone tx: {}", tx.getTxId());
        Await.waitUntilTransactionIsExecuted(tx.getTxId(), Constants.NEOW3J);

        log.info("finishMilestone gas fee: {}", Utils.getGasWithDecimals(tx.getSystemFee() + tx.getNetworkFee()));
    }

    private static String queryWCAJson(SmartContract contract, String trueId) throws Throwable {
        var tx = contract.invokeFunction("queryWCA", ContractParameter.string(trueId))
                .signers(Signer.calledByEntry(Constants.CONTRACT_OWNER_ACCOUNT)).wallet(Constants.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();

        if (response.hasError()) {
            throw new Exception(response.getError().getMessage());
        }

        log.info("queryWCA tx: {}", tx.getTxId());
        Await.waitUntilTransactionIsExecuted(tx.getTxId(), Constants.NEOW3J);
        
        log.info("queryWCA gas fee: {}", Utils.getGasWithDecimals(tx.getSystemFee() + tx.getNetworkFee()));
        
        return tx.getApplicationLog().getExecutions().get(0).getStack().get(0).getString();
    }
}
