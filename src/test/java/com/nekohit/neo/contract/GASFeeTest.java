package com.nekohit.neo.contract;

import io.neow3j.wallet.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test measure the gas usage for each WCA operation.
 * No function is tested.
 * */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GASFeeTest extends ContractTestFramework{
    private final Logger logger = LoggerFactory.getLogger(GASFeeTest.class);
    private final Wallet creatorWallet = getTestWallet();
    private final Wallet buyer1Wallet = getTestWallet();
    private final Wallet buyer2Wallet = getTestWallet();

    @Test
    void test() throws Throwable {
        String[] wcaS = new String[2];
        Long[] wcaL = new Long[2];
        for (int i = 0; i < 2; i++) {
            wcaS[i] = "Large text in content for the Milestone #" + i;
            wcaL[i] = System.currentTimeMillis() + 60 * 1000 * (i + 1) * 2;
        }

        logger.info("Creating WCA...");
        String id = ContractInvokeHelper.createWCA(
                getWcaContract(), "description",
                100, 1000,
                wcaS,
                wcaS,
                wcaL,
                0, 100, false,
                "test_gas_usage_" + System.currentTimeMillis(),
                this.creatorWallet
        );

        logger.info("Pay stake...");
        transferToken(
                getCatToken(), this.creatorWallet,
                getWcaContractAddress(),
                100 * 1000 / 100,
                id, true
        );

        logger.info("User1 buy");
        transferToken(
                getCatToken(), this.buyer1Wallet,
                getWcaContractAddress(),
                200,
                id, true
        );

        logger.info("User2 buy");
        transferToken(
                getCatToken(), this.buyer2Wallet,
                getWcaContractAddress(),
                500,
                id, true
        );

        logger.info("Finish MS#1");
        ContractInvokeHelper.finishMilestone(
                getWcaContract(), id,
                0, "proof-of-work",
                this.creatorWallet
        );

        logger.info("User1 refund");
        ContractInvokeHelper.refund(
                getWcaContract(), id, this.buyer1Wallet
        );

        logger.info("Finish WCA");
        ContractInvokeHelper.finishWCA(
                getWcaContract(), id, this.creatorWallet
        );
    }
}
