package com.nekohit.neo.contract;

import com.nekohit.neo.domain.ExceptionMessages;
import io.neow3j.test.ContractTest;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.wallet.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class test the cancel method for WCA.
 * Including general check(caller, invalid id, double cancel),
 * ok to cancel(pending and open),
 * shouldn't cancel(active and finished).
 */
@ContractTest(blockTime = 1, contracts = {
        CatToken.class,
        WCAContract.class,
})
public class WCACancelTest extends ContractTestFramework {
    private Account creatorAccount;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        creatorAccount = getTestAccount();
        testAccount = getTestAccount();
    }

    @Test
    void testCancelNotFound() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), "some_invalid_id", this.creatorAccount
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.RECORD_NOT_FOUND),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testCancelNotOwner() throws Throwable {
        var identifier = "test_cancel_wca_not_owner" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.declareProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.testAccount
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_SIGNATURE),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testCancelPending() throws Throwable {
        var identifier = "test_cancel_pending" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.declareProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );

        assertDoesNotThrow(
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.creatorAccount
                )
        );
    }

    @Test
    void testCancelOpen() throws Throwable {
        Account buyerAccount1 = getTestAccount();
        Account buyerAccount2 = getTestAccount();
        Account testAccount = getTestAccount();
        var buyer1Purchase = 400_00;
        var buyer2Purchase = 500_00;
        var totalAmount = 1000_00;
        var stakeRate = 10;
        var identifier = "test_cancel_open_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), stakeRate, totalAmount,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, false,
                identifier, testAccount
        );

        // purchase
        // NOTE: one purchase per WCA per block. Since only one write operation will be accepted
        //       by committee, rest of them will be discarded and become invalid.
        transferToken(
                getCatToken(), buyerAccount1,
                getWcaContractAddress(),
                buyer1Purchase, identifier, true
        );
        transferToken(
                getCatToken(), buyerAccount2,
                getWcaContractAddress(),
                buyer2Purchase, identifier, true
        );

        var creatorOldBalance = getCatToken().getBalanceOf(testAccount).longValue();
        var buyer1OldBalance = getCatToken().getBalanceOf(buyerAccount1).longValue();
        var buyer2OldBalance = getCatToken().getBalanceOf(buyerAccount2).longValue();

        ContractInvokeHelper.cancelProject(getWcaContract(), identifier, testAccount);

        var creatorNewBalance = getCatToken().getBalanceOf(testAccount).longValue();
        var buyer1NewBalance = getCatToken().getBalanceOf(buyerAccount1).longValue();
        var buyer2NewBalance = getCatToken().getBalanceOf(buyerAccount2).longValue();

        // buy this time:
        var staked = totalAmount * stakeRate / 100;
        assertEquals(buyer1OldBalance + buyer1Purchase, buyer1NewBalance);
        assertEquals(buyer2OldBalance + buyer2Purchase, buyer2NewBalance);
        assertEquals(creatorOldBalance + staked, creatorNewBalance);
    }

    @Test
    void testCancelActive() throws Throwable {
        var identifier = "test_cancel_active" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 60 * 1000 + 1
                },
                0, 100, false,
                identifier, this.creatorAccount
        );

        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "something",
                this.creatorAccount);

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.creatorAccount
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STAGE_ACTIVE),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testCancelFinished() throws Throwable {
        var identifier = "test_cancel_finished" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );

        ContractInvokeHelper.finishProject(getWcaContract(), identifier, this.creatorAccount);

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.creatorAccount
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STATUS_ALLOW_PENDING_AND_ONGOING),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testDoubleCancel() throws Throwable {
        var identifier = "test_double_cancel" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.declareProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );

        assertDoesNotThrow(
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.creatorAccount
                )
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.creatorAccount
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.RECORD_NOT_FOUND),
                "Unknown exception: " + throwable.getMessage()
        );
    }
}
