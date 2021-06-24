package info.skyblond.nekohit.test.wca;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import info.skyblond.nekohit.test.ContractTestFramework;
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
@ExtendWith(ContractTestFramework.class)
public class WCARefundTest extends ContractTestFramework  {    
    private static final int TEMP_WALLET_BALANCE = 10000_00;

    private Wallet testWallet = Wallet.create();

    @BeforeAll
    void prepareTestAccount() throws Throwable {
        prepareGas(testWallet.getDefaultAccount().getScriptHash(), 10_00000000, false);
        prepareCatToken(testWallet.getDefaultAccount().getScriptHash(), TEMP_WALLET_BALANCE, true);
    }


    @Test
    void testInvalidId() {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                "some_invalid_id", 
                CONTRACT_OWNER_WALLET
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
                    Signer.calledByEntry(CONTRACT_OWNER_WALLET.getDefaultAccount())
                }, 
                CONTRACT_OWNER_WALLET
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
            CONTRACT_OWNER_WALLET
        );
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                identifier, 
                CONTRACT_OWNER_WALLET
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
            CONTRACT_OWNER_WALLET
        );
        ContractInvokeHelper.finishMilestone(
            getWcaContract(), identifier, 0, "proofOfWork", CONTRACT_OWNER_WALLET
        );
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                identifier, 
                CONTRACT_OWNER_WALLET
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
            new Long[] { System.currentTimeMillis() + 3*1000 }, 
            0, 100, "test_refund_last_ms_expired_" + System.currentTimeMillis(),
            CONTRACT_OWNER_WALLET
        );
        Thread.sleep(3*1000);
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                identifier, 
                CONTRACT_OWNER_WALLET
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
            CONTRACT_OWNER_WALLET
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
            CONTRACT_OWNER_WALLET
        );

        ContractInvokeHelper.finishMilestone(
            getWcaContract(), identifier, 0, "proofOfWork", CONTRACT_OWNER_WALLET
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
            CONTRACT_OWNER_WALLET
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
        var identifier = ContractInvokeHelper.createAndPayWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1000_00, 
            new String[]{"milestone1", "milestone2"}, 
            new Long[] { System.currentTimeMillis() + 60*1000, System.currentTimeMillis() + 61*1000 }, 
            0, 100, "test_normal_refund_after_threshold_" + System.currentTimeMillis(),
            CONTRACT_OWNER_WALLET
        );

        var purchaseAmount = 1000_00;
        var oldBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();
        // purchase
        transferToken(
            getCatToken(), testWallet, getWcaContractAddress(), 
            purchaseAmount, identifier, true
        );

        ContractInvokeHelper.finishMilestone(
            getWcaContract(), identifier, 0, "proofOfWork", CONTRACT_OWNER_WALLET
        );

        assertDoesNotThrow(
            () -> ContractInvokeHelper.refund(
                getWcaContract(), 
                identifier, 
                testWallet
            )
        );
        var newBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();
        
        assertEquals(oldBalance - purchaseAmount / 2, newBalance);
    }
}
