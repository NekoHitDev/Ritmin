package info.skyblond.nekohit.neo.contract;

import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;
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
    private final Wallet creatorWallet = getTestWallet();
    private final Wallet testWallet = getTestWallet();

    @Test
    void testInvalidId() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        "some_invalid_id",
                        this.creatorWallet
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
                        new Signer[]{
                                AccountSigner.calledByEntry(this.creatorWallet.getDefaultAccount())
                        },
                        this.creatorWallet
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
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                "test_refund_unpaid_" + System.currentTimeMillis(),
                this.creatorWallet
        );
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.creatorWallet
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
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                "test_refund_last_ms_finished_" + System.currentTimeMillis(),
                this.creatorWallet
        );
        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", this.creatorWallet
        );
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.creatorWallet
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
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 2 * 1000},
                0, 100, false,
                "test_refund_last_ms_expired_" + System.currentTimeMillis(),
                this.creatorWallet
        );
        Thread.sleep(3 * 1000);
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.creatorWallet
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
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                "test_record_404_before_thrshd_" + System.currentTimeMillis(),
                this.creatorWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.testWallet
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
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{System.currentTimeMillis() + 60 * 1000, System.currentTimeMillis() + 61 * 1000},
                0, 100, false,
                "test_record_404_after_thrshd_" + System.currentTimeMillis(),
                this.creatorWallet
        );

        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", this.creatorWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.testWallet
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
                getWcaContract(), "description",
                1_00, 1000_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{System.currentTimeMillis() + 60 * 1000, System.currentTimeMillis() + 61 * 1000},
                0, 100, false,
                "test_normal_refund_before_threshold_" + System.currentTimeMillis(),
                this.creatorWallet
        );
        var oldBalance = getCatToken().getBalanceOf(this.testWallet.getDefaultAccount()).longValue();
        // purchase
        transferToken(
                getCatToken(), this.testWallet, getWcaContractAddress(),
                1000_00, identifier, true
        );

        assertDoesNotThrow(
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.testWallet
                )
        );
        var newBalance = getCatToken().getBalanceOf(this.testWallet.getDefaultAccount()).longValue();

        assertEquals(oldBalance, newBalance);
    }

    @Test
    void testNormalRefundAfterThreshold() throws Throwable {
        var stakeRate = 1_00;
        var identifier = ContractInvokeHelper.createAndPayWCA(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                stakeRate, 1000_00,
                new String[]{"milestone1", "milestone2"},
                new String[]{"milestone1", "milestone2"},
                new Long[]{System.currentTimeMillis() + 60 * 1000, System.currentTimeMillis() + 61 * 1000},
                0, 100, false,
                "test_normal_refund_after_threshold_" + System.currentTimeMillis(),
                this.creatorWallet
        );

        var purchaseAmount = 1000_00;
        var oldBuyerBalance = getCatToken().getBalanceOf(this.testWallet.getDefaultAccount()).longValue();
        var oldCreatorBalance = getCatToken().getBalanceOf(this.creatorWallet.getDefaultAccount()).longValue();
        // purchase
        transferToken(
                getCatToken(), this.testWallet, getWcaContractAddress(),
                purchaseAmount, identifier, true
        );

        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "proofOfWork", this.creatorWallet
        );

        assertDoesNotThrow(
                () -> ContractInvokeHelper.refund(
                        getWcaContract(),
                        identifier,
                        this.testWallet
                )
        );
        var newBuyerBalance = getCatToken().getBalanceOf(this.testWallet.getDefaultAccount()).longValue();
        var newCreatorBalance = getCatToken().getBalanceOf(this.creatorWallet.getDefaultAccount()).longValue();

        assertEquals(oldBuyerBalance - purchaseAmount / 2, newBuyerBalance);
        assertEquals(oldCreatorBalance + purchaseAmount * stakeRate / 100 / 2, newCreatorBalance);
    }
}
