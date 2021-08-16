package info.skyblond.nekohit.neo.contract;

import info.skyblond.nekohit.neo.domain.Messages;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.wallet.Wallet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                () -> ContractInvokeHelper.cancelWCA(
                        getWcaContract(), "some_invalid_id", this.creatorWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(Messages.RECORD_NOT_FOUND),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testCancelNotOwner() throws Throwable {
        var identifier = "test_cancel_wca_not_owner" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createWCA(
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.cancelWCA(
                        getWcaContract(), identifier, this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(Messages.INVALID_SIGNATURE),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testCancelPending() {
        Assertions.assertTrue(false);
    }

    @Test
    void testCancelOpen() {
        Assertions.assertTrue(false);
    }

    @Test
    void testCancelActive() {
        Assertions.assertTrue(false);
    }

    @Test
    void testCancelFinished() {
        Assertions.assertTrue(false);
    }

    @Test
    void testDoubleCancel() {
        Assertions.assertTrue(false);
    }
}
