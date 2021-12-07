package com.nekohit.neo.contract;

import com.nekohit.neo.domain.ExceptionMessages;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.wallet.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class test the refund method for WCA.
 * Including invalid id, invalid signer, unpaid, last ms is finished, last ms expired.
 * record not found, normal op(before and after threshold)
 */
@TestInstance(Lifecycle.PER_CLASS)
public class WCARefundTest extends ContractTestFramework {
    private final Account creatorAccount = getTestAccount();
    private final Account testAccount = getTestAccount();

    @Test
    void testInvalidId() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        "some_invalid_id",
                        this.creatorAccount
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.RECORD_NOT_FOUND),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidSigner() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractTestFramework.invokeFunction(
                        getWcaContract(), "refund",
                        new ContractParameter[]{
                                ContractParameter.string("identifier"),
                                ContractParameter.hash160(Account.create())
                        },
                        new Signer[]{
                                AccountSigner.calledByEntry(this.creatorAccount)
                        }
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_SIGNATURE),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testRefundUnpaid() throws Throwable {
        var identifier = ContractInvokeHelper.declareProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                "test_refund_unpaid_" + System.currentTimeMillis(),
                this.creatorAccount
        );
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.creatorAccount
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testLastMilestoneFinished() throws Throwable {
        var identifier = ContractInvokeHelper.createAndPayProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                "test_refund_last_ms_finished_" + System.currentTimeMillis(),
                this.creatorAccount
        );
        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", this.creatorAccount
        );
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.creatorAccount
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testLastMilestoneExpired() throws Throwable {
        var identifier = ContractInvokeHelper.createAndPayProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 2 * 1000},
                0, 100, false,
                "test_refund_last_ms_expired_" + System.currentTimeMillis(),
                this.creatorAccount
        );
        Thread.sleep(3 * 1000);
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.creatorAccount
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STAGE_READY_TO_FINISH),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testRecordNotFoundBeforeThreshold() throws Throwable {
        var identifier = ContractInvokeHelper.createAndPayProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                "test_record_404_before_thrshd_" + System.currentTimeMillis(),
                this.creatorAccount
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.testAccount
                )
        );

        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.RECORD_NOT_FOUND),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testRecordNotFoundAfterThreshold() throws Throwable {
        var identifier = ContractInvokeHelper.createAndPayProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{System.currentTimeMillis() + 60 * 1000, System.currentTimeMillis() + 61 * 1000},
                0, 100, false,
                "test_record_404_after_thrshd_" + System.currentTimeMillis(),
                this.creatorAccount
        );

        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", this.creatorAccount
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.testAccount
                )
        );

        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.RECORD_NOT_FOUND),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNormalRefundBeforeThreshold() throws Throwable {
        var identifier = ContractInvokeHelper.createAndPayProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1000_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{System.currentTimeMillis() + 60 * 1000, System.currentTimeMillis() + 61 * 1000},
                0, 100, false,
                "test_normal_refund_before_threshold_" + System.currentTimeMillis(),
                this.creatorAccount
        );
        var oldBalance = getCatToken().getBalanceOf(this.testAccount).longValue();
        // purchase
        transferToken(
                getCatToken(), this.testAccount, getWcaContractAddress(),
                1000_00, identifier, true
        );

        assertDoesNotThrow(
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.testAccount
                )
        );
        var newBalance = getCatToken().getBalanceOf(this.testAccount).longValue();

        assertEquals(oldBalance, newBalance);
    }

    @Test
    void testNormalRefundAfterThreshold() throws Throwable {
        var stakeRate = 1_00;
        var identifier = ContractInvokeHelper.createAndPayProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), stakeRate, 1000_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{System.currentTimeMillis() + 60 * 1000, System.currentTimeMillis() + 61 * 1000},
                0, 100, false,
                "test_normal_refund_after_threshold_" + System.currentTimeMillis(),
                this.creatorAccount
        );

        var purchaseAmount = 1000_00;
        var oldBuyerBalance = getCatToken().getBalanceOf(this.testAccount).longValue();
        var oldCreatorBalance = getCatToken().getBalanceOf(this.creatorAccount).longValue();
        // purchase
        transferToken(
                getCatToken(), this.testAccount, getWcaContractAddress(),
                purchaseAmount, identifier, true
        );

        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", this.creatorAccount
        );

        assertDoesNotThrow(
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.testAccount
                )
        );
        var newBuyerBalance = getCatToken().getBalanceOf(this.testAccount).longValue();
        var newCreatorBalance = getCatToken().getBalanceOf(this.creatorAccount).longValue();

        assertEquals(oldBuyerBalance - purchaseAmount / 2, newBuyerBalance);
        assertEquals(oldCreatorBalance + purchaseAmount * stakeRate / 100 / 2, newCreatorBalance);
    }
}
