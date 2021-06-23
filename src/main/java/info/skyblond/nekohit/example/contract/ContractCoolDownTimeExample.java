package info.skyblond.nekohit.example.contract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import info.skyblond.nekohit.example.Constants;
import info.skyblond.nekohit.example.Utils;

/**
 * This file demonstrate how a refund operation would be:
 * Create, stake, purchase, finish milestones, refund before threshold,
 * finish some mileston, then refund after threshold, and finish WCA, 
 * distribute token after finish. The WCA will create as 
 * CONTRACT_OWNER account, and the buyer will be random 
 * generated wallets.
 * 
 * By this demo: WCA: 5 stage, 3 buyer. Buyer[0] refund before threshold, 
 * so he will end up with 1000_00 tokens, no lost, no win.
 * Buyer[1] refund at ms[1], so only 1/5 of his token will lose, no stake is involved.
 * Buyer[2] make no refund, so he will lose 2/5 for finished ms, 
 * and win the partial stake.
 */
public class ContractCoolDownTimeExample {
    /**
     * Stake rate, i.e. Stake per 1_00 token
     */
    private static final int STAKE_PER_100_TOKEN = 10;

    /**
     * Milestones, true means milestone will be finished, 
     * false means not.
     */
    private static final String[] MILESTONES = new String[] {
        "ms1", "ms2", "ms3"
    };

    /**
     * The threshold milestone's index
     */
    private static final int THRESHOLD_MILESTONE_INDEX = 1;

    private static final Logger logger = LoggerFactory.getLogger(ContractCoolDownTimeExample.class);

    public static void main(String[] args) throws Throwable {
        var contract = Constants.WCA_CONTRACT;
        String[] descriptions = new String[MILESTONES.length];
        for (int i = 0; i < descriptions.length; i++) {
            descriptions[i] = "ms" + i;
        }
        Long[] endTimestamps = new Long[MILESTONES.length];


        logger.info("Creating WCA...");
        String trueId = ContractHelper.createWCA(
            contract, STAKE_PER_100_TOKEN, 100, 
            descriptions, endTimestamps, THRESHOLD_MILESTONE_INDEX, 
            // cool down time 10min = 600s = 600_000ms
            600_000, "test_cool_down_" + System.currentTimeMillis()
        );
        logger.info("created WCA with ID: {}", trueId);
        logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));


        long totalStake = STAKE_PER_100_TOKEN;
        logger.info("Paying stake, total {}", Utils.getCatWithDecimals(totalStake));
        Utils.transferCatToken(Constants.CONTRACT_OWNER_WALLET, contract.getScriptHash(), totalStake, trueId);
        logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));


        // finish WCA
        logger.info("Finishing milestones...");
        for (int i = 0; i < MILESTONES.length; i++) {
            logger.info("Finishing milestone {}", i);
            ContractHelper.finishMilestone(contract, trueId, i, descriptions[i] + " Finished!");
            logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));
        }
        // By finishing milestone[1], since we have to wait 10min to finish next one
        // there will be a excpetion in this loop to demo the cool-down feature.
    }
}
