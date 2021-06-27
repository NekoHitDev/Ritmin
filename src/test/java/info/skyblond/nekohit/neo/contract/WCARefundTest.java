package info.skyblond.nekohit.neo.contract;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

/**
 * This class test the refund method for WCA. 
 * Including invalid id, invalid signer, unpaid, last ms is finished, last ms expired.
 * record not found, normal op(before and after threshold)
 */
@TestInstance(Lifecycle.PER_CLASS)
public class WCARefundTest extends ContractTestFramework  {
    private Wallet creatorWallet = getTestWallet();
    private Wallet testWallet = getTestWallet();

    @Test
    void testInvalidId() {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                "some_invalid_id", 
                creatorWallet
            )
        );
        assertTrue(
            throwable.getMessage().contains("Identifier not found."),
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
                new Signer[] {
                    Signer.calledByEntry(creatorWallet.getDefaultAccount())
                }, 
                creatorWallet
            )
        );
        assertTrue(
            throwable.getMessage().contains("Invalid sender signature."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testRefundUnpaid() throws Throwable {
        var identifier = ContractInvokeHelper.createWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone"}, 
            new Long[] { System.currentTimeMillis() + 60*1000 }, 
            0, 100, "test_refund_unpaid_" + System.currentTimeMillis(),
            creatorWallet
        );
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                identifier, 
                creatorWallet
            )
        );
        assertTrue(
            throwable.getMessage().contains("You can not refund an unpaid WCA."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testLastMilestoneFinished() throws Throwable {
        var identifier = ContractInvokeHelper.createAndPayWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1"}, 
            new Long[] { System.currentTimeMillis() + 60*1000 }, 
            0, 100, "test_refund_last_ms_finished_" + System.currentTimeMillis(),
            creatorWallet
        );
        ContractInvokeHelper.finishMilestone(
            getWcaContract(), identifier, 0, "proofOfWork", creatorWallet
        );
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                identifier, 
                creatorWallet
            )
        );
        assertTrue(
            throwable.getMessage().contains("You can not refund a finished WCA."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testLastMilestoneExpired() throws Throwable {
        var identifier = ContractInvokeHelper.createAndPayWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1"}, 
            new Long[] { System.currentTimeMillis() + 2 * 1000 }, 
            0, 100, "test_refund_last_ms_expired_" + System.currentTimeMillis(),
            creatorWallet
        );
        Thread.sleep(3*1000);
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                identifier, 
                creatorWallet
            )
        );
        assertTrue(
            throwable.getMessage().contains("You can not refund a finished WCA."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testRecordNotFoundBeforeThreshold() throws Throwable {
        var identifier = ContractInvokeHelper.createAndPayWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1"}, 
            new Long[] { System.currentTimeMillis() + 60*1000 }, 
            0, 100, "test_record_404_before_thrshd_" + System.currentTimeMillis(),
            creatorWallet
        );

        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                identifier, 
                testWallet
            )
        );

        assertTrue(
            throwable.getMessage().contains("Purchase not found"),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testRecordNotFoundAfterThreshold() throws Throwable {
        var identifier = ContractInvokeHelper.createAndPayWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1", "milestone2"}, 
            new Long[] { System.currentTimeMillis() + 60*1000, System.currentTimeMillis() + 61*1000 }, 
            0, 100, "test_record_404_after_thrshd_" + System.currentTimeMillis(),
            creatorWallet
        );

        ContractInvokeHelper.finishMilestone(
            getWcaContract(), identifier, 0, "proofOfWork", creatorWallet
        );

        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                identifier, 
                testWallet
            )
        );

        assertTrue(
            throwable.getMessage().contains("Purchase not found"),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNormalRefundBeforeThreshold() throws Throwable {
        var identifier = ContractInvokeHelper.createAndPayWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1000_00, 
            new String[]{"milestone1", "milestone2"}, 
            new Long[] { System.currentTimeMillis() + 60*1000, System.currentTimeMillis() + 61*1000 }, 
            0, 100, "test_normal_refund_before_threshold_" + System.currentTimeMillis(),
            creatorWallet
        );
        var oldBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();
        // purchase
        transferToken(
            getCatToken(), testWallet, getWcaContractAddress(), 
            1000_00, identifier, true
        );

        assertDoesNotThrow(
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                identifier, 
                testWallet
            )
        );
        var newBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();
        
        assertEquals(oldBalance, newBalance);
    }

    @Test
    void testNormalRefundAfterThreshold() throws Throwable {
        var stakeRate = 1_00;
        var identifier = ContractInvokeHelper.createAndPayWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), stakeRate, 1000_00, 
            new String[]{"milestone1", "milestone2"}, 
            new Long[] { System.currentTimeMillis() + 60*1000, System.currentTimeMillis() + 61*1000 }, 
            0, 100, "test_normal_refund_after_threshold_" + System.currentTimeMillis(),
            creatorWallet
        );

        var purchaseAmount = 1000_00;
        var oldBuyerBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();
        var oldCreatorBalance = getCatToken().getBalanceOf(creatorWallet.getDefaultAccount()).longValue();
        // purchase
        transferToken(
            getCatToken(), testWallet, getWcaContractAddress(), 
            purchaseAmount, identifier, true
        );

        ContractInvokeHelper.finishMilestone(
            getWcaContract(), identifier, 0, "proofOfWork", creatorWallet
        );

        assertDoesNotThrow(
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                identifier, 
                testWallet
            )
        );
        var newBuyerBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();
        var newCreatorBalance = getCatToken().getBalanceOf(creatorWallet.getDefaultAccount()).longValue();
        
        assertEquals(oldBuyerBalance - purchaseAmount / 2, newBuyerBalance);
        assertEquals(oldCreatorBalance + purchaseAmount * stakeRate / 100 / 2, newCreatorBalance);
    }
}