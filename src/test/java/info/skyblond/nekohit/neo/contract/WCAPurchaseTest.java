package info.skyblond.nekohit.neo.contract;

import info.skyblond.nekohit.neo.domain.Messages;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.wallet.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class test the on payment method for WCA.
 * Including general check(caller, invalid id),
 * pay stake(duplicate paid, expired wca, wrong amount, normal op),
 * purchase(unpaid, already start, first ms not finished but expired, insufficient remain,
 * normal op(one shot, multiple purchase)).
 */
@TestInstance(Lifecycle.PER_CLASS)
public class WCAPurchaseTest extends ContractTestFramework {
    private final Wallet creatorWallet = getTestWallet();
    private final Wallet testWallet = getTestWallet();

    @Test
    void invalidCallerTest() {
        var tempWallet = Wallet.create();
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> invokeFunction(
                        getWcaContract(), "onNEP17Payment",
                        new ContractParameter[]{
                                ContractParameter.hash160(tempWallet.getDefaultAccount()),
                                ContractParameter.integer(100),
                                ContractParameter.string("some_id")
                        },
                        new Signer[]{
                                AccountSigner.calledByEntry(tempWallet.getDefaultAccount())
                        },
                        tempWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(Messages.INVALID_CALLER),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidId() throws Throwable {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> transferToken(
                        getCatToken(),
                        this.creatorWallet,
                        getWcaContractAddress(),
                        1000,
                        "some_invalid_id",
                        false
                )
        );
        assertTrue(
                throwable.getMessage().contains(Messages.RECORD_NOT_FOUND),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testDoublePayStake() throws Throwable {
        var identifier = "test_double_pay_stake_" + System.currentTimeMillis();
        // create and pay WCA
        ContractInvokeHelper.createAndPayWCA(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );
        // pay again
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> transferToken(
                        getCatToken(), this.creatorWallet,
                        getWcaContractAddress(),
                        1_00, identifier, false
                )
        );
        assertTrue(
                throwable.getMessage().contains(Messages.INVALID_STATUS_ALLOW_PENDING),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPayWrongAmount() throws Throwable {
        var identifier = "test_pay_wrong_amount_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createWCA(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );
        // pay
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> transferToken(
                        getCatToken(), this.creatorWallet,
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
    void testNormalPayStake() throws Throwable {
        var identifier = "test_normal_pay_stake_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createWCA(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );
        // pay
        assertDoesNotThrow(
                () -> transferToken(
                        getCatToken(), this.creatorWallet,
                        getWcaContractAddress(),
                        1_00, identifier, false
                )
        );
    }

    @Test
    void testPurchaseUnpaid() throws Throwable {
        var identifier = "test_purchase_unpaid_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createWCA(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );
        // purchase
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> transferToken(
                        getCatToken(), this.testWallet,
                        getWcaContractAddress(),
                        10, identifier, false
                )
        );
        assertTrue(
                throwable.getMessage().contains(Messages.INVALID_STATUS_ALLOW_OPEN_AND_ACTIVE),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPurchaseReadyToFinish() throws Throwable {
        var identifier = "test_purchase_finish_" + System.currentTimeMillis();
        var firstEndTimestamp = System.currentTimeMillis() + 3 * 1000;
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{firstEndTimestamp},
                0, 100, false,
                identifier, this.creatorWallet
        );
        // wait for first milestone expire
        while (System.currentTimeMillis() <= firstEndTimestamp) {
            Thread.sleep(100);
        }
        // purchase
        var throwable = assertThrows(
                Exception.class,
                () -> transferToken(
                        getCatToken(), this.testWallet,
                        getWcaContractAddress(),
                        10, identifier, false
                )
        );
        assertTrue(
                throwable.getMessage().contains(Messages.INVALID_STATUS_READY_TO_FINISH),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPurchaseInsufficient() throws Throwable {
        var identifier = "test_purchase_insufficient_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );

        // purchase
        var throwable = assertThrows(
                Exception.class,
                () -> transferToken(
                        getCatToken(), this.testWallet,
                        getWcaContractAddress(),
                        1000_00, identifier, false
                )
        );
        assertTrue(
                throwable.getMessage().contains("Insufficient token remain in this WCA."),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testOneShotPurchase() throws Throwable {
        var identifier = "test_one_shot_purchase_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, 1000_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );
        // purchase
        assertDoesNotThrow(
                () -> transferToken(
                        getCatToken(), this.testWallet,
                        getWcaContractAddress(),
                        1000_00, identifier, true
                )
        );
    }

    @Test
    void testMultiPurchase() throws Throwable {
        var identifier = "test_multi_purchase_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, 1000_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );
        // purchase
        assertDoesNotThrow(
                () -> transferToken(
                        getCatToken(), this.testWallet,
                        getWcaContractAddress(),
                        500_00, identifier, false
                )
        );
        assertDoesNotThrow(
                () -> transferToken(
                        getCatToken(), this.testWallet,
                        getWcaContractAddress(),
                        500_00, identifier, true
                )
        );
    }
}
