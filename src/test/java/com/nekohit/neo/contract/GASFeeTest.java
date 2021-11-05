package com.nekohit.neo.contract;

import io.neow3j.wallet.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test measure the gas usage for each WCA operation.
 * No function is tested.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GASFeeTest extends ContractTestFramework {
    private final Logger logger = LoggerFactory.getLogger(GASFeeTest.class);
    private final Wallet creatorWallet = getTestWallet();
    private final Wallet buyer1Wallet = getTestWallet();
    private final Wallet buyer2Wallet = getTestWallet();

    @Test
    void test() throws Throwable {
        String[] wcaS = new String[10];
        Long[] wcaL = new Long[10];
        for (int i = 0; i < 10; i++) {
            wcaS[i] = "Large text in content for the Milestone #" + i;
            wcaL[i] = System.currentTimeMillis() + 60 * 1000 * (i + 1) * 2;
        }

        this.logger.info("Creating WCA...");
        String id = ContractInvokeHelper.declareProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 100, 1000,
                wcaS, wcaS, wcaL,
                0, 100, false,
                "test_gas_usage_" + System.currentTimeMillis(),
                this.creatorWallet
        );

        this.logger.info("Pay stake...");
        transferToken(
                getCatToken(), this.creatorWallet,
                getWcaContractAddress(),
                100 * 1000 / 100,
                id, true
        );

        this.logger.info("User1 buy");
        transferToken(
                getCatToken(), this.buyer1Wallet,
                getWcaContractAddress(),
                200,
                id, true
        );

        this.logger.info("User2 buy");
        transferToken(
                getCatToken(), this.buyer2Wallet,
                getWcaContractAddress(),
                500,
                id, true
        );

        this.logger.info("Finish MS#1");
        ContractInvokeHelper.finishMilestone(
                getWcaContract(), id,
                0, "proof-of-work",
                this.creatorWallet
        );

        this.logger.info("User1 refund");
        ContractInvokeHelper.refund(
                getWcaContract(), id, this.buyer1Wallet
        );

        this.logger.info("Finish WCA");
        ContractInvokeHelper.finishProject(
                getWcaContract(), id, this.creatorWallet
        );
    }
}
