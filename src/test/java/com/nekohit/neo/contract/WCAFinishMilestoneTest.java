package com.nekohit.neo.contract;

import com.nekohit.neo.domain.ExceptionMessages;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.wallet.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class test the finish milestone method for WCA.
 * Including invalid id, invalid signer, unpaid,
 * cool down time not met, missed ms, finished ms, expired ms,
 * proof of work is null, normal op
 */
@TestInstance(Lifecycle.PER_CLASS)
public class WCAFinishMilestoneTest extends ContractTestFramework {
    private final Wallet testWallet = getTestWallet();

    @Test
    void testInvalidId() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishMilestone(
                        getWcaContract(), "some_invalid_id", 0, "proofOfWork", this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.RECORD_NOT_FOUND),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidSigner() throws Throwable {
        var identifier = "test_invalid_owner_sign_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.testWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishMilestone(
                        getWcaContract(), identifier, 0, "proofOfWork", Wallet.create()
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_SIGNATURE),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishUnpaid() throws Throwable {
        var identifier = "test_finish_ms_unpaid_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.declareProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, true,
                identifier, this.testWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishMilestone(
                        getWcaContract(), identifier, 0, "proofOfWork", this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testCoolDownInterval() throws Throwable {
        var identifier = "test_cool_down_interval_not_met_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{System.currentTimeMillis() + 60 * 1000, System.currentTimeMillis() + 61 * 1000},
                0, 60 * 1000, false,
                identifier, this.testWallet
        );

        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", this.testWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishMilestone(
                        getWcaContract(), identifier, 1, "proofOfWork", this.testWallet
                )
        );

        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.COOL_DOWN_TIME_NOT_MET),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishMissedMilestone() throws Throwable {
        var identifier = "test_finish_misses_milestone_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, false,
                identifier, this.testWallet
        );

        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 1, "proofOfWork", this.testWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishMilestone(
                        getWcaContract(), identifier, 0, "proofOfWork", this.testWallet
                )
        );

        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_MILESTONE_PASSED),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testDoubleFinishMilestone() throws Throwable {
        var identifier = "test_double_finish_milestone_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000
                },
                0, 1, false,
                identifier, this.testWallet
        );

        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", this.testWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishMilestone(
                        getWcaContract(), identifier, 0, "proofOfWork", this.testWallet
                )
        );

        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_MILESTONE_PASSED),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishExpired() throws Throwable {
        var identifier = "test_finish_expired_milestone_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{
                        System.currentTimeMillis() + 3 * 1000,
                        System.currentTimeMillis() + 61 * 1000
                },
                0, 1, false,
                identifier, this.testWallet
        );

        Thread.sleep(3 * 1000);

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishMilestone(
                        getWcaContract(), identifier, 0, "proofOfWork", this.testWallet
                )
        );

        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_MILESTONE_EXPIRED),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNullProofOfWork() throws Throwable {
        var identifier = "test_finish_null_proof_of_work_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000
                },
                0, 1, false,
                identifier, this.testWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishMilestone(
                        getWcaContract(), identifier, 0, null, this.testWallet
                )
        );

        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_PROOF_OF_WORK),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNormalFinish() throws Throwable {
        var identifier = "test_finish_normal_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000
                },
                0, 1, false,
                identifier, this.testWallet
        );

        assertDoesNotThrow(
                () -> ContractInvokeHelper.finishMilestone(
                        getWcaContract(), identifier, 0, "Somethine", this.testWallet
                )
        );
    }
}
