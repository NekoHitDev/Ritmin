package info.skyblond.nekohit.test.wca;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import io.neow3j.wallet.Wallet;

/**
 * This class test the on payment method for WCA. 
 * Including general check(caller, invalid id), 
 * pay stake(duplicate paid, expired wca, wrong amount, normal op), 
 * purchase(unpaid, already start, first ms not finished but expired, insufficient remain, 
 *     normal op(one shot, multiple purchase)).
 */
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(ContractTestFramework.class)
public class WCAPurchaseTest extends ContractTestFramework  {
    
    private static final int TEMP_WALLET_BALANCE = 10000_00;

    private Wallet testWallet = Wallet.create();

    @BeforeAll
    void prepareTestAccount() throws Throwable {
        prepareGas(testWallet.getDefaultAccount().getScriptHash(), 10_00000000, false);
        prepareCatToken(testWallet.getDefaultAccount().getScriptHash(), TEMP_WALLET_BALANCE, true);
    }


    @Test
    void invalidCallerTest() {
        var tempWallet = Wallet.create();
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> invokeFunction(
                getWcaContract(), "onNEP17Payment", 
                new ContractParameter[] {
                    ContractParameter.hash160(tempWallet.getDefaultAccount()),
                    ContractParameter.integer(100),
                    ContractParameter.string("some_id")
                },
                new Signer[] {
                    Signer.calledByEntry(tempWallet.getDefaultAccount())
                },
                tempWallet
            )
        );
        assertTrue(
            throwable.getMessage().contains("Only Cat Token can invoke this function."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidId() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> transferToken(
                getCatToken(), 
                CONTRACT_OWNER_WALLET,
                getWcaContractAddress(), 
                1000,
                "some_invalid_id",
                false
            )
        );
        assertTrue(
            throwable.getMessage().contains("Identifier not found."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testDoublePayStake() throws Throwable {
        var identifier = "test_double_pay_stake_" + System.currentTimeMillis();
        // create and pay WCA
        ContractInvokeHelper.createAndPayWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone"}, 
            new Long[] {System.currentTimeMillis() + 60 * 1000}, 
            0, 100, identifier, 
            CONTRACT_OWNER_WALLET
        );
        // pay again
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> transferToken(
                getCatToken(), CONTRACT_OWNER_WALLET,
                getWcaContractAddress(), 
                1_00, identifier, false
            )
        );
        assertTrue(
            throwable.getMessage().contains("You can't pay a paid WCA."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPayExpiredWCA() throws Throwable {
        var identifier = "test_pay_expired_" + System.currentTimeMillis();
        var lastEndTimestamp = System.currentTimeMillis() + 3 * 1000;
        // create WCA
        ContractInvokeHelper.createWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone"}, 
            new Long[] { lastEndTimestamp }, 
            0, 100, identifier, 
            CONTRACT_OWNER_WALLET
        );
        // wait for last milestone expire
        while(System.currentTimeMillis() <= lastEndTimestamp) Thread.sleep(100);
        // pay
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> transferToken(
                getCatToken(), CONTRACT_OWNER_WALLET,
                getWcaContractAddress(), 
                1_00, identifier, false
            )
        );
        assertTrue(
            throwable.getMessage().contains("You can't pay a expired WCA."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPayWrongAmount() throws Throwable {
        var identifier = "test_pay_wrong_amount_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone"}, 
            new Long[] { System.currentTimeMillis() + 60*1000 }, 
            0, 100, identifier, 
            CONTRACT_OWNER_WALLET
        );
        // pay
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> transferToken(
                getCatToken(), CONTRACT_OWNER_WALLET,
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
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone"}, 
            new Long[] { System.currentTimeMillis() + 60*1000 }, 
            0, 100, identifier, 
            CONTRACT_OWNER_WALLET
        );
        // pay
        assertDoesNotThrow(
            () -> transferToken(
                getCatToken(), CONTRACT_OWNER_WALLET,
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
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone"}, 
            new Long[] { System.currentTimeMillis() + 60*1000 }, 
            0, 100, identifier, 
            CONTRACT_OWNER_WALLET
        );
        // purchase
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> transferToken(
                getCatToken(), testWallet,
                getWcaContractAddress(), 
                10, identifier, false
            )
        );
        assertTrue(
            throwable.getMessage().contains("You can't buy an unpaid WCA."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPurchaseStarted() throws Throwable {
        var identifier = "test_purchase_started_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1" , "milestone2"}, 
            new Long[] { System.currentTimeMillis() + 30*1000, System.currentTimeMillis() + 60*1000 }, 
            0, 100, identifier, 
            CONTRACT_OWNER_WALLET
        );
        // finish ms[0]
        ContractInvokeHelper.finishMilestone(
            getWcaContract(), identifier, 0, "proofOfWork", CONTRACT_OWNER_WALLET
        );
        // purchase
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> transferToken(
                getCatToken(), testWallet,
                getWcaContractAddress(), 
                10, identifier, false
            )
        );
        assertTrue(
            throwable.getMessage().contains("You can't buy a WCA already started."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPurchaseExpired() throws Throwable {
        var identifier = "test_purchase_expired_" + System.currentTimeMillis();
        var firstEndTimestamp = System.currentTimeMillis() + 3 * 1000;
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1"}, 
            new Long[] { firstEndTimestamp }, 
            0, 100, identifier, 
            CONTRACT_OWNER_WALLET
        );
        // wait for first milestone expire
        while(System.currentTimeMillis() <= firstEndTimestamp) Thread.sleep(100);
        // purchase
        var throwable = assertThrows(
            Exception.class, 
            () -> transferToken(
                getCatToken(), testWallet,
                getWcaContractAddress(), 
                10, identifier, false
            )
        );
        assertTrue(
            throwable.getMessage().contains("You can't buy a WCA already started."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testPurchaseInsufficient() throws Throwable {
        var identifier = "test_purchase_insufficient_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1"}, 
            new Long[] { System.currentTimeMillis() + 60 * 1000 }, 
            0, 100, identifier, 
            CONTRACT_OWNER_WALLET
        );

        // purchase
        var throwable = assertThrows(
            Exception.class, 
            () -> transferToken(
                getCatToken(), testWallet,
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
            getWcaContract(), 1_00, 1000_00, 
            new String[]{"milestone"}, 
            new Long[] { System.currentTimeMillis() + 60*1000 }, 
            0, 100, identifier, 
            CONTRACT_OWNER_WALLET
        );
        // purchase
        assertDoesNotThrow(
            () -> transferToken(
                getCatToken(), testWallet,
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
            getWcaContract(), 1_00, 1000_00, 
            new String[]{"milestone"}, 
            new Long[] { System.currentTimeMillis() + 60*1000 }, 
            0, 100, identifier, 
            CONTRACT_OWNER_WALLET
        );
        // purchase
        assertDoesNotThrow(
            () -> transferToken(
                getCatToken(), testWallet,
                getWcaContractAddress(), 
                500_00, identifier, false
            )
        );
        assertDoesNotThrow(
            () -> transferToken(
                getCatToken(), testWallet,
                getWcaContractAddress(), 
                500_00, identifier, true
            )
        );
    }
}
