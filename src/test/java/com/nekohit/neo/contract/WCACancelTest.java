package com.nekohit.neo.contract;

import com.nekohit.neo.domain.ExceptionMessages;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.wallet.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class test the cancel method for WCA.
 * Including general check(caller, invalid id, double cancel),
 * ok to cancel(pending and open),
 * shouldn't cancel(active and finished).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WCACancelTest extends ContractTestFramework {
    private final Wallet creatorWallet = getTestWallet();
    private final Wallet testWallet = getTestWallet();

    @Test
    void testCancelNotFound() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), "some_invalid_id", this.creatorWallet
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
                identifier, this.creatorWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.testWallet
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
                identifier, this.creatorWallet
        );

        assertDoesNotThrow(
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.creatorWallet
                )
        );
    }

    @Test
    void testCancelOpen() throws Throwable {
        Wallet buyerWallet1 = getTestWallet();
        Wallet buyerWallet2 = getTestWallet();
        Wallet testWallet = getTestWallet();
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
                identifier, testWallet
        );

        // purchase
        // NOTE: one purchase per WCA per block. Since only one write operation will be accepted
        //       by committee, rest of them will be discarded and become invalid.
        transferToken(
                getCatToken(), buyerWallet1,
                getWcaContractAddress(),
                buyer1Purchase, identifier, true
        );
        transferToken(
                getCatToken(), buyerWallet2,
                getWcaContractAddress(),
                buyer2Purchase, identifier, true
        );

        var creatorOldBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();
        var buyer1OldBalance = getCatToken().getBalanceOf(buyerWallet1.getDefaultAccount()).longValue();
        var buyer2OldBalance = getCatToken().getBalanceOf(buyerWallet2.getDefaultAccount()).longValue();

        ContractInvokeHelper.cancelProject(getWcaContract(), identifier, testWallet);

        var creatorNewBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();
        var buyer1NewBalance = getCatToken().getBalanceOf(buyerWallet1.getDefaultAccount()).longValue();
        var buyer2NewBalance = getCatToken().getBalanceOf(buyerWallet2.getDefaultAccount()).longValue();

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
                identifier, this.creatorWallet
        );

        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "something",
                this.creatorWallet);

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.creatorWallet
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
                identifier, this.creatorWallet
        );

        ContractInvokeHelper.finishProject(getWcaContract(), identifier, this.creatorWallet);

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.creatorWallet
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
                identifier, this.creatorWallet
        );

        assertDoesNotThrow(
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.creatorWallet
                )
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.cancelProject(
                        getWcaContract(), identifier, this.creatorWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.RECORD_NOT_FOUND),
                "Unknown exception: " + throwable.getMessage()
        );
    }
}
