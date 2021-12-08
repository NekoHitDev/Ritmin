package com.nekohit.neo.contract;

import com.nekohit.neo.domain.ExceptionMessages;
import io.neow3j.test.ContractTest;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.wallet.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class test the on payment method for WCA.
 * Including general check(caller, invalid id),
 * pay stake(duplicate paid, expired wca, wrong amount, normal op),
 * purchase(unpaid, already start, first ms not finished but expired, insufficient remain,
 * normal op(one shot, multiple purchase)).
 */
@ContractTest(blockTime = 1, contracts = {
        CatToken.class,
        WCAContract.class,
})
public class WCAPurchaseTest extends ContractTestFramework {
    private Account creatorAccount;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        creatorAccount = getTestAccount();
        testAccount = getTestAccount();
    }

    @Test
    void testInvalidId() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> transferToken(
                        getCatToken(),
                        this.creatorAccount,
                        getWcaContractAddress(),
                        1000,
                        "some_invalid_id",
                        false
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.RECORD_NOT_FOUND),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testDoublePayStake() throws Throwable {
        var identifier = "test_double_pay_stake_" + System.currentTimeMillis();
        // create and pay WCA
        ContractInvokeHelper.createAndPayProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );
        // pay again
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> transferToken(
                        getCatToken(), this.creatorAccount,
                        getWcaContractAddress(),
                        1_00, identifier, false
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STATUS_ALLOW_PENDING),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPayWrongAmount() throws Throwable {
        var identifier = "test_pay_wrong_amount_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.declareProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );
        // pay
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> transferToken(
                        getCatToken(), this.creatorAccount,
                        getWcaContractAddress(),
                        10, identifier, false
                )
        );
        assertTrue(
                throwable.getMessage().contains("Amount not correct"),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testWrongTokenPayStake() throws Throwable {
        var identifier = "test_wrong_token_pay_stake_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.declareProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );
        // pay
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> transferToken(
                        gasToken, this.creatorAccount,
                        getWcaContractAddress(),
                        1_00, identifier, false
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_CALLER),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNormalPayStake() throws Throwable {
        var identifier = "test_normal_pay_stake_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.declareProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );
        // pay
        assertDoesNotThrow(
                () -> transferToken(
                        getCatToken(), this.creatorAccount,
                        getWcaContractAddress(),
                        1_00, identifier, false
                )
        );
    }

    @Test
    void testPurchaseUnpaid() throws Throwable {
        var identifier = "test_purchase_unpaid_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.declareProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );
        // purchase
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> transferToken(
                        getCatToken(), this.testAccount,
                        getWcaContractAddress(),
                        10, identifier, false
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPurchaseWrongToken() throws Throwable {
        var identifier = "test_purchase_wrong_token_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.declareProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );
        // purchase
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> transferToken(
                        gasToken, this.testAccount,
                        getWcaContractAddress(),
                        10, identifier, false
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_CALLER),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPurchaseReadyToFinish() throws Throwable {
        var identifier = "test_purchase_finish_" + System.currentTimeMillis();
        var firstEndTimestamp = System.currentTimeMillis() + 3 * 1000;
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{firstEndTimestamp},
                0, 100, false,
                identifier, this.creatorAccount
        );
        // wait for first milestone expire
        while (System.currentTimeMillis() <= firstEndTimestamp) {
            //noinspection BusyWait
            Thread.sleep(100);
        }
        // purchase
        var throwable = assertThrows(
                Exception.class,
                () -> transferToken(
                        getCatToken(), this.testAccount,
                        getWcaContractAddress(),
                        10, identifier, false
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STAGE_READY_TO_FINISH),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPurchaseInsufficient() throws Throwable {
        var identifier = "test_purchase_insufficient_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );

        // purchase
        var throwable = assertThrows(
                Exception.class,
                () -> transferToken(
                        getCatToken(), this.testAccount,
                        getWcaContractAddress(),
                        1000_00, identifier, false
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INSUFFICIENT_AMOUNT_REMAIN),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testOneShotPurchase() throws Throwable {
        var identifier = "test_one_shot_purchase_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1000_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );
        // purchase
        assertDoesNotThrow(
                () -> transferToken(
                        getCatToken(), this.testAccount,
                        getWcaContractAddress(),
                        1000_00, identifier, true
                )
        );
    }

    @Test
    void testMultiPurchase() throws Throwable {
        var identifier = "test_multi_purchase_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1000_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );
        // purchase
        assertDoesNotThrow(
                () -> transferToken(
                        getCatToken(), this.testAccount,
                        getWcaContractAddress(),
                        500_00, identifier, false
                )
        );
        assertDoesNotThrow(
                () -> transferToken(
                        getCatToken(), this.testAccount,
                        getWcaContractAddress(),
                        500_00, identifier, true
                )
        );
    }
}
