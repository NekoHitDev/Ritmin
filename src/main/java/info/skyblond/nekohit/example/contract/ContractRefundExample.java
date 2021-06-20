package info.skyblond.nekohit.example.contract;

import java.io.IOException;

import static info.skyblond.nekohit.neo.helper.Utils.require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import info.skyblond.nekohit.example.Constants;
import info.skyblond.nekohit.example.Utils;
import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.wallet.Wallet;

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
public class ContractRefundExample {
    /**
     * There will be 3 buyers: 
     * refund before threshold, refund after threshold and don't refund
     */
    private static final int TOTAL_BUYER_COUNT = 3;

    /**
     * How many token each buyer will purchase
     */
    private static final long TOKEN_PER_PURCHASE = 1000_00;

    /**
     * Stake rate, i.e. Stake per 1_00 token
     */
    private static final int STAKE_PER_100_TOKEN = 10;

    /**
     * Milestones, true means milestone will be finished, 
     * false means not.
     */
    private static final boolean[] MILESTONES = new boolean[] {
        true, false, true, false, true
    };

    /**
     * The threshold milestone's index
     */
    private static final int THRESHOLD_MILESTONE_INDEX = 2;

    private static final Logger logger = LoggerFactory.getLogger(ContractRefundExample.class);

    public static void main(String[] args) throws Throwable {
        // This demo requires threshold and the last ms must be finished
        require(MILESTONES[THRESHOLD_MILESTONE_INDEX], "Threshold ms must be finished");
        require(MILESTONES[MILESTONES.length - 1], "Last ms must be finished");
        require(MILESTONES.length == 5, "Must be 5 milestones, otherwise the refund logic should change");
        var contract = Constants.WCA_CONTRACT;
        String[] descriptions = new String[MILESTONES.length];
        for (int i = 0; i < descriptions.length; i++) {
            descriptions[i] = "ms" + i;
        }
        Long[] endTimestamps = new Long[MILESTONES.length];
        long totalAmount = TOTAL_BUYER_COUNT * TOKEN_PER_PURCHASE;


        logger.info("Creating WCA...");
        String trueId = ContractHelper.createWCA(
            contract, STAKE_PER_100_TOKEN, totalAmount, 
            descriptions, endTimestamps, THRESHOLD_MILESTONE_INDEX, 
            "test_refund_" + System.currentTimeMillis()
        );
        logger.info("created WCA with ID: {}", trueId);
        logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));
        queryContractOwnerCatBalance();


        long totalStake = totalAmount * STAKE_PER_100_TOKEN / 100;
        logger.info("Paying stake, total {}", Utils.getCatWithDecimals(totalStake));
        Utils.transferCatToken(Constants.CONTRACT_OWNER_WALLET, contract.getScriptHash(), totalStake, trueId);
        logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));
        queryContractOwnerCatBalance();


        Wallet[] testWallets = new Wallet[TOTAL_BUYER_COUNT];
        logger.info("Preparing test wallet");
        queryContractOwnerCatBalance();
        for (int i = 0; i < testWallets.length; i++) {
            testWallets[i] = Utils.prepaWallet(TOKEN_PER_PURCHASE, Wallet.create());
            logger.info("Test {} cat balance: {}", i, Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(testWallets[i].getDefaultAccount().getScriptHash())));
        }
        queryContractOwnerCatBalance();


        logger.info("Making purchase...");
        for (int j = 0; j < testWallets.length; j++) {
            Wallet testWallet = testWallets[j];
            Utils.transferCatToken(testWallet, contract.getScriptHash(), TOKEN_PER_PURCHASE, trueId);
            logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));
            logger.info("Test {} cat balance: {}", j, Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));
        }


        // finish WCA
        logger.info("Finishing milestones...");
        for (int i = 0; i < MILESTONES.length; i++) {
            if (!MILESTONES[i]) {
                logger.info("Skipping milestone {}", i);
            } else {
                logger.info("Finishing milestone {}", i);
                ContractHelper.finishMilestone(contract, trueId, i, descriptions[i] + " Finished!");
                logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));
            }
            if (i == 1) {
                // refund buyer[0] before threshold, this should be a full refund
                logger.info("Refunding buyer[0] before threshold...");
                ContractHelper.refund(contract, trueId, testWallets[0]);
                logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));
                logger.info("Test {} cat balance: {}", 0, Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(testWallets[0].getDefaultAccount().getScriptHash())));
            }
            if (i == 3) {
                // refund buyer[1] after threshold, this should be a partial refund
                logger.info("Refunding buyer[1] after threshold...");
                ContractHelper.refund(contract, trueId, testWallets[1]);
                logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));
                logger.info("Test {} cat balance: {}", 1, Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(testWallets[1].getDefaultAccount().getScriptHash())));
            }
        }
        // if last milestone is not finished, then we have to manually end it
        if (!MILESTONES[MILESTONES.length - 1]) {
            ContractHelper.finishWCA(contract, trueId);
            logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));
        }
        queryContractOwnerCatBalance();
        for (int j = 0; j < testWallets.length; j++) {
            Wallet testWallet = testWallets[j];
            logger.info("Test {} cat balance: {}", j, Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));
        }
    }

    private static void queryContractOwnerCatBalance() throws UnexpectedReturnTypeException, IOException{
        logger.info("Owner cat balance: {}",
            Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(Constants.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
    }
}
