package info.skyblond.nekohit.example.contract;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.skyblond.nekohit.example.Constants;
import info.skyblond.nekohit.example.Utils;
import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.wallet.Wallet;

/**
 * This file demonstrate how a normal operation would be:
 * Create, stake, purchase, finish milestones, distribute token after finish.
 * The WCA will create as CONTRACT_OWNER account, and the buyer will be random 
 * generated wallets.
 */
public class ContractNormalExample {
    /**
     * How many buyers in total
     */
    private static final int TOTAL_BUYER_COUNT = 10;

    /**
     * How many token each buyer will purchase
     */
    private static final long TOKEN_PER_PURCHASE = 1000_00;

    /**
     * Stake rate, i.e. Stake per 1_00 token
     */
    private static final int STAKE_PER_100_TOKEN = 10;

    /**
     * How many perchase to dump all tokens in. 1 for one shot.
     */
    private static final int PURCHASE_COUNT = 2;

    /**
     * How many milestones are here, true means milestone will be finished, 
     * false means not.
     */
    private static final boolean[] MILESTONES = new boolean[] {
        true, true, false, false, true, 
        true, true, true, false, true
    };

    /**
     * The threshold milestone's index
     */
    private static final int THRESHOLD_MILESTONE_INDEX = 3;

    private static final Logger logger = LoggerFactory.getLogger(ContractNormalExample.class);

    public static void main(String[] args) throws Throwable {
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
            1, "test_normal_operation_" + System.currentTimeMillis()
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
        // for COUNT - 1, each purchase TOTAL / COUNT
        for (int i = 0; i < PURCHASE_COUNT - 1; i++) {
            logger.info("Purchase {}/{}", i + 1, PURCHASE_COUNT);
            for (int j = 0; j < testWallets.length; j++) {
                Wallet testWallet = testWallets[j];
                Utils.transferCatToken(testWallet, contract.getScriptHash(), TOKEN_PER_PURCHASE / PURCHASE_COUNT, trueId);
                logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));
                logger.info("Test {} cat balance: {}", j, Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));
            }
        }
        // for the last time, dump all the rest
        logger.info("Purchase {}/{}", PURCHASE_COUNT, PURCHASE_COUNT);
        var rest = TOKEN_PER_PURCHASE - TOKEN_PER_PURCHASE / PURCHASE_COUNT * (PURCHASE_COUNT - 1);
        for (int j = 0; j < testWallets.length; j++) {
            Wallet testWallet = testWallets[j];
            Utils.transferCatToken(testWallet, contract.getScriptHash(), rest, trueId);
            logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));
            logger.info("Test {} cat balance: {}", j, Utils.getCatWithDecimals(Constants.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));
        }


        // finish WCA
        logger.info("Finishing milestones...");
        for (int i = 0; i < MILESTONES.length; i++) {
            if (!MILESTONES[i]) {
                logger.info("Skipping milestone {}", i);
                continue;
            }
            logger.info("Finishing milestone {}", i);
            ContractHelper.finishMilestone(contract, trueId, i, descriptions[i] + " Finished!");
            logger.info("WCA info: {}", ContractHelper.queryWCAJson(contract, trueId));
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
