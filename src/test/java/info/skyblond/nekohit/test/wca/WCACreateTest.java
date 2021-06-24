package info.skyblond.nekohit.test.wca;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import info.skyblond.nekohit.test.ContractTestFramework;
import io.neow3j.types.ContractParameter;
import io.neow3j.wallet.Account;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;

/**
 * This class test the creation of WCA. 
 * Including invalid parameter, invalid milestones, duplicate identifier, etc.
 */
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(ContractTestFramework.class)
public class WCACreateTest extends ContractTestFramework {
    @Test
    void testNegativeStakeRate() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), -100, 10, 
                new String[]{"milestone"}, 
                new Long[] {System.currentTimeMillis() + 60 * 1000}, 
                0, 100, "test_negative_stake_rate_" + System.currentTimeMillis(), 
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("The stake amount per 100 token must be positive."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testZeroStakeRate() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), 0, 10, 
                new String[]{"milestone"}, 
                new Long[] {System.currentTimeMillis() + 60 * 1000}, 
                0, 100, "test_zero_stake_rate_" + System.currentTimeMillis(), 
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("The stake amount per 100 token must be positive."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNegativeTokenCount() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), 100, -100, 
                new String[]{"milestone"}, 
                new Long[] {System.currentTimeMillis() + 60 * 1000}, 
                0, 100, "test_negative_token_count_" + System.currentTimeMillis(), 
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("The max sell token count must be positive."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testZeroTokenCount() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), 100, 0, 
                new String[]{"milestone"}, 
                new Long[] {System.currentTimeMillis() + 60 * 1000}, 
                0, 100, "test_zero_token_count_" + System.currentTimeMillis(), 
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("The max sell token count must be positive."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidSigner() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> {
                ContractTestFramework.invokeFunction(
                    getWcaContract(), "createWCA", 
                    new ContractParameter[]{
                        ContractParameter.hash160(Account.create()),
                        ContractParameter.integer(100), 
                        ContractParameter.integer(1000),
                        ContractParameter.array(Arrays.asList(new String[]{"milestone"})),
                        ContractParameter.array(Arrays.asList(new Long[] {System.currentTimeMillis() + 60 * 1000})),
                        ContractParameter.integer(0),
                        ContractParameter.integer(100),
                        ContractParameter.string("test_invalid_signer_" + System.currentTimeMillis())
                    }, 
                    new Signer[] {
                        Signer.calledByEntry(CONTRACT_OWNER_WALLET.getDefaultAccount())
                    }, 
                    CONTRACT_OWNER_WALLET
                );
            }
        );
        assertTrue(
            throwable.getMessage().contains("Invalid sender signature."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testDuplicateIdentifier() throws Throwable {
        var identifier = "test_duplicate_id_" + System.currentTimeMillis();
        ContractInvokeHelper.createWCA(
            getWcaContract(), 100, 1000, 
            new String[]{"milestone"}, 
            new Long[] {System.currentTimeMillis() + 60 * 1000}, 
            0, 100, identifier, 
            CONTRACT_OWNER_WALLET
        );
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), 100, 0, 
                new String[]{"milestone"}, 
                new Long[] {System.currentTimeMillis() + 60 * 1000}, 
                0, 100, identifier, 
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("Duplicate identifier."),
            "Unknown exception: " + throwable.getMessage()
        );
    }
    
    @Test
    void testDifferentMilestoneCount() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), 100, 1000, 
                new String[]{"milestone1", "milestone2"}, 
                new Long[] {System.currentTimeMillis() + 60 * 1000}, 
                0, 100, "test_different_milestone_count_" + System.currentTimeMillis(), 
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("Cannot decide milestones count."),
            "Unknown exception: " + throwable.getMessage()
        );
    }
    
    @Test
    void testDecreaseEndTimestamp() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), 100, 1000, 
                new String[]{"milestone1", "milestone2"}, 
                new Long[] {System.currentTimeMillis() + 60 * 1000, System.currentTimeMillis() + 59 * 1000}, 
                0, 100, "test_decrease_end_timestamp_" + System.currentTimeMillis(), 
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("The end timestamp should increase."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testExpiredEndTimestamp() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), 100, 1000, 
                new String[]{"milestone1"}, 
                // definitely a expired timestamp
                new Long[] { 12345L }, 
                0, 100, "test_expired_end_timestamp_" + System.currentTimeMillis(), 
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("The end timestamp is already expired."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    // TODO cannot test zero milestone, 
    //      since neow3j require at least 1 element in a given array

    @Test
    void testInvalidThresholdMilestone() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), 100, 1000, 
                new String[]{"milestone1"}, 
                new Long[] { System.currentTimeMillis() + 60 * 1000 }, 
                -1, 100, "test_invalid_threshold_milestone_" + System.currentTimeMillis(), 
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("Invalid value for thresholdIndex"),
            "Unknown exception: " + throwable.getMessage()
        );

        throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), 100, 1000, 
                new String[]{"milestone1"}, 
                new Long[] { System.currentTimeMillis() + 60 * 1000 }, 
                1, 100, "test_invalid_threshold_milestone_" + System.currentTimeMillis(), 
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("Invalid value for thresholdIndex"),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidCoolDownInterval() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class, 
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), 100, 1000, 
                new String[]{"milestone1"}, 
                new Long[] { System.currentTimeMillis() + 60 * 1000 }, 
                0, -100, "test_invalid_cool_down_interval_" + System.currentTimeMillis(), 
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("Cool down interval must not be negative."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNormalOperation() throws Throwable {
        assertDoesNotThrow(
            () -> ContractInvokeHelper.createWCA(
                getWcaContract(), 100, 1000, 
                new String[]{"milestone1"}, 
                new Long[] { System.currentTimeMillis() + 60 * 1000 }, 
                0, 100, "test_normal_" + System.currentTimeMillis(), 
                CONTRACT_OWNER_WALLET
            )
        );
    }
}
