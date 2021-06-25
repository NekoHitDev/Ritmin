package info.skyblond.nekohit.test.wca;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import info.skyblond.nekohit.test.ContractTestFramework;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.wallet.Wallet;

/**
 * This class test the finish milestone method for WCA. 
 * Including invalid id, invalid signer, unpaid, 
 * cool down time not met, missed ms, finished ms, expired ms,
 * proof of work is null, normal op
 */
@TestInstance(Lifecycle.PER_CLASS)
public class WCAFinishMilestoneTest extends ContractTestFramework  {
    private Wallet testWallet = getTestWallet();
    @Test
    void testInvalidId(){
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.finishMilestone(
                getWcaContract(), "some_invalid_id", 0, "proofOfWork", testWallet
            )
        );
        assertTrue(
            throwable.getMessage().contains("Identifier not found."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidSigner() throws Throwable{
        var identifier = "test_invalid_owner_sign_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1"}, 
            new Long[] { System.currentTimeMillis() + 60 * 1000 }, 
            0, 100, identifier, 
            testWallet
        );

        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", Wallet.create()
            )
        );
        assertTrue(
            throwable.getMessage().contains("Invalid caller signature."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishUnpaid() throws Throwable{
        var identifier = "test_finish_ms_unpaid_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createWCA(
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1"}, 
            new Long[] { System.currentTimeMillis() + 60 * 1000 }, 
            0, 100, identifier, 
            testWallet
        );

        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", testWallet
            )
        );
        assertTrue(
            throwable.getMessage().contains("You can't finish an unpaid WCA."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testCoolDownInterval() throws Throwable{
        var identifier = "test_cool_down_interval_not_met_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1", "milestone2"}, 
            new Long[] { System.currentTimeMillis() + 60 * 1000, System.currentTimeMillis() + 61 * 1000 }, 
            0, 60 * 1000, identifier, 
            testWallet
        );

        ContractInvokeHelper.finishMilestone(
            getWcaContract(), identifier, 0, "proofOfWork", testWallet
        );

        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 1, "proofOfWork", testWallet
            )
        );

        assertTrue(
            throwable.getMessage().contains("Cool down time not met"),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishMissedMilestone() throws Throwable{
        var identifier = "test_finish_misses_milestone_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1", "milestone2", "milestone3"}, 
            new Long[] {
                System.currentTimeMillis() + 60 * 1000, 
                System.currentTimeMillis() + 61 * 1000,
                System.currentTimeMillis() + 62 * 1000
            }, 
            0, 1, identifier, 
            testWallet
        );

        ContractInvokeHelper.finishMilestone(
            getWcaContract(), identifier, 1, "proofOfWork", testWallet
        );

        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", testWallet
            )
        );

        assertTrue(
            throwable.getMessage().contains("You can't finish a passed milestone"),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testDoubleFinishMilestone() throws Throwable{
        var identifier = "test_double_finish_milestone_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1", "milestone2"}, 
            new Long[] {
                System.currentTimeMillis() + 60 * 1000, 
                System.currentTimeMillis() + 61 * 1000
            }, 
            0, 1, identifier, 
            testWallet
        );

        ContractInvokeHelper.finishMilestone(
            getWcaContract(), identifier, 0, "proofOfWork", testWallet
        );

        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", testWallet
            )
        );

        assertTrue(
            throwable.getMessage().contains("You can't finish a passed milestone"),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishExpired() throws Throwable{
        var identifier = "test_finish_expired_milestone_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1", "milestone2"}, 
            new Long[] {
                System.currentTimeMillis() + 3 * 1000, 
                System.currentTimeMillis() + 61 * 1000
            }, 
            0, 1, identifier, 
            testWallet
        );

        Thread.sleep(3 * 1000);

        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", testWallet
            )
        );

        assertTrue(
            throwable.getMessage().contains("You can't finish a expired milestone"),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNullProofOfWork() throws Throwable{
        var identifier = "test_finish_null_proof_of_work_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1", "milestone2"}, 
            new Long[] {
                System.currentTimeMillis() + 60 * 1000, 
                System.currentTimeMillis() + 61 * 1000
            }, 
            0, 1, identifier, 
            testWallet
        );

        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, null, testWallet
            )
        );

        assertTrue(
            throwable.getMessage().contains("Proof of work must be valid."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNormalFinish() throws Throwable{
        var identifier = "test_finish_normal_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1", "milestone2"}, 
            new Long[] {
                System.currentTimeMillis() + 60 * 1000, 
                System.currentTimeMillis() + 61 * 1000
            }, 
            0, 1, identifier, 
            testWallet
        );

        assertDoesNotThrow(
            () -> ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "Somethine", testWallet
            )
        );
    }
}
